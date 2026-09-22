#!/usr/bin/env python3

from __future__ import annotations

import io
import json
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch


TRANSLATIONS_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = TRANSLATIONS_DIR.parents[1]
sys.path.insert(0, str(TRANSLATIONS_DIR))

from android_resources import (  # noqa: E402
    AndroidResource,
    ResourceSet,
    ResourceValidationError,
    android_resource_qualifier,
    audit_locale,
    baseline_existing_translations,
    discover_resource_sets,
    load_state,
    make_batches,
    merge_translations,
    parse_batch_response,
    read_failure_artifact,
    read_resource_file,
    update_state,
    write_resource_file,
    write_failure_artifact,
    write_state,
)
from languages import TranslationTarget  # noqa: E402
from providers.base import TranslationResult  # noqa: E402
import translate_app_resources as app_translations  # noqa: E402
from translate_app_resources import _select_reference_payload, _source_text  # noqa: E402


SOURCE_XML = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="hello">Hello</string>
    <string name="welcome">Welcome, %1$s!\\nReady?</string>
    <string name="not_translated" translatable="false">internal-id</string>
    <string-array name="modes">
        <item>Light</item>
        <item>Dark</item>
    </string-array>
    <plurals name="things">
        <item quantity="one">%d Thing</item>
        <item quantity="other">%d Things</item>
    </plurals>
</resources>
"""


TARGET_XML = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="hello">Bonjour</string>
    <string name="target_only">À supprimer</string>
    <string name="not_translated">identifiant-interne</string>
</resources>
"""


def string_resource(source_set: str, name: str, text: str) -> AndroidResource:
    element = ET.Element("string", {"name": name})
    element.text = text
    return AndroidResource(
        source_set=source_set,
        resource_type="string",
        name=name,
        attributes={"name": name},
        value={"text": text},
        element=element,
    )


class AndroidResourcesTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.resource_set = ResourceSet("test-main", Path("module/src/main/res"))
        self.target = TranslationTarget("fr", "French")
        source = self.resource_set.source_path(self.root)
        source.parent.mkdir(parents=True)
        source.write_text(SOURCE_XML, encoding="utf-8")
        target = self.resource_set.target_path(self.root, self.target)
        target.parent.mkdir(parents=True)
        target.write_text(TARGET_XML, encoding="utf-8")

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def audit(self, state: dict[str, object] | None = None):
        return audit_locale(
            self.root,
            self.target,
            (self.resource_set,),
            state or {"version": 2, "translations": {}},
        )

    def test_audit_uses_english_as_authority_and_lists_target_only_resources(self) -> None:
        audit = self.audit()
        self.assertEqual(
            (
                "test-main/plurals:things",
                "test-main/string-array:modes",
                "test-main/string:welcome",
            ),
            audit.missing,
        )
        self.assertEqual(
            (
                "test-main/string:not_translated",
                "test-main/string:target_only",
            ),
            audit.deletion_candidates,
        )
        self.assertNotIn("test-main/string:target_only", [item.key for item in audit.pending])

    def test_resource_sets_are_discovered_from_android_source_tree(self) -> None:
        strings = self.root / "app/new-module/src/newFlavor/res/values/strings.xml"
        strings.parent.mkdir(parents=True)
        strings.write_text("<resources/>", encoding="utf-8")
        discovered = discover_resource_sets(self.root)
        self.assertEqual(1, len(discovered))
        self.assertEqual("app/new-module/src/newFlavor", discovered[0].identifier)
        self.assertEqual(
            Path("app/new-module/src/newFlavor/res"), discovered[0].relative_res_dir
        )

    def test_changelog_viewer_owns_no_translatable_resources(self) -> None:
        viewer_sets = [
            resource_set
            for resource_set in discover_resource_sets(PROJECT_ROOT)
            if resource_set.identifier.startswith("app/changelog-viewer/")
        ]
        self.assertTrue(viewer_sets)
        for resource_set in viewer_sets:
            resources = read_resource_file(
                resource_set.source_path(PROJECT_ROOT), resource_set.identifier
            ).resources.values()
            self.assertTrue(all(not resource.is_translatable for resource in resources))

    def test_existing_translation_without_state_is_stale(self) -> None:
        self.assertIn("test-main/string:hello", self.audit().stale)

    def test_baseline_associates_existing_translation_with_current_english(self) -> None:
        state: dict[str, object] = {"version": 2, "translations": {}}
        self.assertEqual(
            1,
            baseline_existing_translations(
                self.root, (self.target,), (self.resource_set,), state
            ),
        )
        audit = self.audit(state)
        self.assertNotIn("test-main/string:hello", audit.stale)
        self.assertIn(
            "test-main/string:hello",
            [source.key for source, _translation in audit.references],
        )

    def test_generated_translation_becomes_stale_when_english_changes(self) -> None:
        source_file = read_resource_file(
            self.resource_set.source_path(self.root), self.resource_set.identifier
        )
        source = source_file.resources["test-main/string:hello"]
        state = {
            "version": 2,
            "translations": {
                "fr": {
                    source.key: "old-source-hash",
                }
            },
        }
        audit = self.audit(state)
        self.assertEqual((source.key,), audit.stale)
        self.assertNotIn(source.key, [item.key for item, _translation in audit.references])

    def test_target_edit_is_preserved_until_english_changes(self) -> None:
        source = read_resource_file(
            self.resource_set.source_path(self.root), self.resource_set.identifier
        ).resources["test-main/string:hello"]
        state = {
            "version": 2,
            "translations": {
                "fr": {
                    source.key: source.source_hash,
                }
            },
        }
        self.assertNotIn(source.key, self.audit(state).stale)
        source_path = self.resource_set.source_path(self.root)
        source_path.write_text(SOURCE_XML.replace("Hello", "Hello there"), encoding="utf-8")
        self.assertIn(source.key, self.audit(state).stale)

    def test_response_validation_accepts_target_specific_plural_categories(self) -> None:
        resources = {resource.key: resource for resource in self.audit().pending}
        plural = resources["test-main/plurals:things"]
        response = json.dumps(
            {
                "translations": [
                    {
                        "id": plural.key,
                        "value": [
                            {"quantity": "one", "value": {"text": "%d chose"}},
                            {"quantity": "many", "value": {"text": "%d choses"}},
                            {"quantity": "other", "value": {"text": "%d choses"}},
                        ],
                    }
                ]
            }
        )
        parsed = parse_batch_response((plural,), response)
        self.assertIn(plural.key, parsed)

    def test_response_validation_rejects_changed_placeholder(self) -> None:
        welcome = next(resource for resource in self.audit().pending if resource.name == "welcome")
        response = json.dumps(
            {
                "translations": [
                    {"id": welcome.key, "value": {"text": "Bienvenue !\\nPrêt ?"}}
                ]
            }
        )
        with self.assertRaisesRegex(ResourceValidationError, "placeholders"):
            parse_batch_response((welcome,), response)

    def test_response_normalizes_android_apostrophe_escaping(self) -> None:
        hello = read_resource_file(
            self.resource_set.source_path(self.root), self.resource_set.identifier
        ).resources["test-main/string:hello"]
        response = json.dumps(
            {
                "translations": [
                    {"id": hello.key, "value": {"text": "Aujourd'hui"}}
                ]
            }
        )
        parsed = parse_batch_response((hello,), response)
        self.assertEqual({"text": "Aujourd\\'hui"}, parsed[hello.key])

    def test_merge_preserves_deletion_candidates_until_explicit_cleanup(self) -> None:
        audit = self.audit()
        welcome = next(resource for resource in audit.pending if resource.name == "welcome")
        target_path = self.resource_set.target_path(self.root, self.target)
        target_file = read_resource_file(target_path, self.resource_set.identifier)
        translated = {welcome.key: (welcome, {"text": "Bienvenue, %1$s!\\nPrêt ?"})}
        write_resource_file(target_path, merge_translations(target_file, translated))
        updated = read_resource_file(target_path, self.resource_set.identifier)
        self.assertIn("test-main/string:target_only", updated.resources)
        self.assertEqual(
            {"text": "Bienvenue, %1$s!\\nPrêt ?"},
            updated.resources[welcome.key].value,
        )

    def test_generated_state_round_trips_and_marks_changed_source_stale(self) -> None:
        welcome = next(resource for resource in self.audit().pending if resource.name == "welcome")
        translated = {welcome.key: (welcome, {"text": "Bienvenue, %1$s!\\nPrêt ?"})}
        target_path = self.resource_set.target_path(self.root, self.target)
        target_file = read_resource_file(target_path, self.resource_set.identifier)
        write_resource_file(target_path, merge_translations(target_file, translated))
        state: dict[str, object] = {"version": 2, "translations": {}}
        update_state(state, self.target, translated)
        state_path = self.root / "state.json"
        write_state(state_path, state)
        loaded = load_state(state_path)
        self.assertNotIn(welcome.key, self.audit(loaded).stale)

        source_path = self.resource_set.source_path(self.root)
        source_path.write_text(SOURCE_XML.replace("Welcome,", "Hello,"), encoding="utf-8")
        self.assertIn(welcome.key, self.audit(loaded).stale)

    def test_batches_are_bounded_without_splitting_resources(self) -> None:
        pending = self.audit().pending
        batches = make_batches(pending, max_characters=180)
        self.assertGreater(len(batches), 1)
        self.assertEqual([item.key for item in pending], [item.key for batch in batches for item in batch])

    def test_request_includes_bounded_existing_translation_memory(self) -> None:
        source_file = read_resource_file(
            self.resource_set.source_path(self.root), self.resource_set.identifier
        )
        hello = source_file.resources["test-main/string:hello"]
        welcome = source_file.resources["test-main/string:welcome"]
        payload = json.loads(
            _source_text((welcome,), [(hello, {"text": "Bonjour"})], 1000)
        )
        self.assertEqual("Bonjour", payload["reference_translations"][0]["translation"]["text"])
        self.assertEqual(welcome.key, payload["resources"][0]["id"])
        without_memory = json.loads(
            _source_text((welcome,), [(hello, {"text": "Bonjour"})], 0)
        )
        self.assertEqual([], without_memory["reference_translations"])

    def test_reference_selection_prioritizes_relevance_independent_of_input_order(self) -> None:
        batch = (string_resource("app-main", "track_mood_graph", "Track your mood graph"),)
        references = [
            (string_resource("app-main", "colour_picker", "Choose a colour"), {"text": "Couleur"}),
            (string_resource("ui-main", "edit_graph", "Edit graph"), {"text": "Modifier"}),
            (
                string_resource("app-main", "tracked_mood_graph", "Your tracked mood graph"),
                {"text": "Graphique d’humeur"},
            ),
        ]

        selected = _select_reference_payload(batch, references, 10_000)
        reversed_selected = _select_reference_payload(batch, list(reversed(references)), 10_000)

        self.assertEqual("app-main/string:tracked_mood_graph", selected[0]["id"])
        self.assertEqual(
            [item["id"] for item in selected],
            [item["id"] for item in reversed_selected],
        )

    def test_reference_fallback_is_deterministically_mixed_across_source_sets(self) -> None:
        batch = (string_resource("batch", "novel_action", "Entirely novel wording"),)
        references = [
            (string_resource("main", "alpha", "First unrelated text"), {"text": "A"}),
            (string_resource("main", "beta", "Second unrelated text"), {"text": "B"}),
            (string_resource("play", "gamma", "Third unrelated text"), {"text": "C"}),
            (string_resource("ui", "delta", "Fourth unrelated text"), {"text": "D"}),
        ]

        selected = _select_reference_payload(batch, references, 10_000)

        self.assertEqual(
            {"main", "play", "ui"},
            {item["id"].split("/", 1)[0] for item in selected[:3]},
        )

    def test_reference_selection_honours_limit_and_skips_oversized_entries(self) -> None:
        batch = (string_resource("batch", "graph", "Graph"),)
        references = [
            (
                string_resource("main", "graph_explanation", "Graph " + "detail " * 100),
                {"text": "Très long"},
            ),
            (string_resource("ui", "save", "Save"), {"text": "Enregistrer"}),
        ]
        limit = 130

        selected = _select_reference_payload(batch, references, limit)
        encoded_size = len(
            json.dumps(selected, ensure_ascii=False, separators=(",", ":"))
        ) - 2

        self.assertLessEqual(encoded_size, limit)
        self.assertEqual(["ui/string:save"], [item["id"] for item in selected])

    def test_new_locales_use_bcp47_android_qualifiers(self) -> None:
        self.assertEqual("de-rDE", android_resource_qualifier("de"))
        self.assertEqual("b+fil", android_resource_qualifier("fil"))
        self.assertEqual("b+zh+Hans", android_resource_qualifier("zh-Hans"))
        self.assertEqual("b+zh+Hant", android_resource_qualifier("zh-Hant"))

    def test_translate_command_writes_validated_incremental_results_and_state(self) -> None:
        class EchoTranslator:
            def translate(self, *, source_text: str, **_kwargs: object) -> TranslationResult:
                source = json.loads(source_text)
                return TranslationResult(
                    text=json.dumps(
                        {
                            "translations": [
                                {"id": item["id"], "value": item["value"]}
                                for item in source["resources"]
                            ]
                        }
                    ),
                    usage={"input_tokens": 1, "output_tokens": 1},
                )

        state_path = self.root / "state.json"
        domain_brief = self.root / "domain.md"
        domain_brief.write_text("Track & Graph test context", encoding="utf-8")
        arguments = [
            "translate_app_resources.py",
            "--project-root", str(self.root),
            "--state", str(state_path),
            "translate",
            "--target", "fr=French",
            "--domain-brief", str(domain_brief),
        ]
        with (
            patch.object(
                app_translations,
                "discover_resource_sets",
                return_value=(self.resource_set,),
            ),
            patch.object(app_translations, "create_translator", return_value=EchoTranslator()),
            patch.object(sys, "argv", arguments),
            redirect_stdout(io.StringIO()),
        ):
            self.assertEqual(0, app_translations.main())

        audit = self.audit(load_state(state_path))
        self.assertEqual((), audit.missing)
        self.assertEqual((), audit.stale)
        self.assertEqual(
            (
                "test-main/string:not_translated",
                "test-main/string:target_only",
            ),
            audit.deletion_candidates,
        )

    def test_invalid_response_is_retained_as_build_breaking_failure_artifact(self) -> None:
        class InvalidTranslator:
            def translate(self, **_kwargs: object) -> TranslationResult:
                return TranslationResult(text="not json", usage={})

        state_path = self.root / "state.json"
        domain_brief = self.root / "domain.md"
        domain_brief.write_text("Track & Graph test context", encoding="utf-8")
        arguments = [
            "translate_app_resources.py",
            "--project-root", str(self.root),
            "--state", str(state_path),
            "translate",
            "--target", "fr=French",
            "--domain-brief", str(domain_brief),
        ]
        stdout = io.StringIO()
        with (
            patch.object(
                app_translations,
                "discover_resource_sets",
                return_value=(self.resource_set,),
            ),
            patch.object(
                app_translations,
                "create_translator",
                return_value=InvalidTranslator(),
            ),
            patch.object(sys, "argv", arguments),
            redirect_stdout(stdout),
        ):
            self.assertEqual(1, app_translations.main())

        audit = self.audit(load_state(state_path))
        self.assertEqual(1, len(audit.failure_artifacts))
        artifact = self.root / audit.failure_artifacts[0]
        self.assertTrue(artifact.read_text(encoding="utf-8").startswith("TRANSLATION_BATCH_FAILED"))
        with self.assertRaises(ET.ParseError):
            ET.parse(artifact)
        payload = read_failure_artifact(artifact)
        self.assertEqual(2, payload["version"])
        self.assertEqual(
            set(audit.missing) | set(audit.stale), set(payload["unrecovered_ids"])
        )
        xml_preview = artifact.read_text(encoding="utf-8").split("\n", 1)[1]
        preview_root = ET.fromstring(xml_preview)
        self.assertEqual("resources", preview_root.tag)
        raw_response = preview_root.find(
            "{http://schemas.android.com/tools}rawProviderResponse"
        )
        self.assertEqual("not json", raw_response.text)
        result = json.loads(stdout.getvalue().splitlines()[-1])
        self.assertEqual(1, result["summary"]["failed_batches"])

    def test_repairable_json_failure_is_saved_as_xml_and_can_be_applied_offline(self) -> None:
        pending = self.audit().pending
        valid_response = json.dumps(
            {
                "translations": [
                    {"id": resource.key, "value": resource.value}
                    for resource in pending
                ]
            },
            separators=(",", ":"),
        )
        malformed_response = valid_response.replace("}},{", "},{", 1)
        artifact = write_failure_artifact(
            project_root=self.root,
            target=self.target,
            resource_set=self.resource_set,
            resources=pending,
            error="malformed JSON",
            provider_response=malformed_response,
        )
        payload = read_failure_artifact(artifact)
        self.assertEqual([], payload["unrecovered_ids"])

        state_path = self.root / "state.json"
        arguments = [
            "translate_app_resources.py",
            "--project-root", str(self.root),
            "--state", str(state_path),
            "apply-failure", str(artifact),
        ]
        with (
            patch.object(
                app_translations,
                "discover_resource_sets",
                return_value=(self.resource_set,),
            ),
            patch.object(sys, "argv", arguments),
            redirect_stdout(io.StringIO()),
        ):
            self.assertEqual(0, app_translations.main())

        self.assertFalse(artifact.exists())
        self.assertEqual((), self.audit(load_state(state_path)).pending)


if __name__ == "__main__":
    unittest.main()
