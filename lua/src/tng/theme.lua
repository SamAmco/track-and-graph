local M = {}

local theme_color = function(name)
	return "THEME_" .. name
end

--- Theme-aware colors, resolved by the app at render time.
--- @since (API level 2)
M.color = {
	primary = theme_color("PRIMARY"),
	on_primary = theme_color("ON_PRIMARY"),
	secondary = theme_color("SECONDARY"),
	on_secondary = theme_color("ON_SECONDARY"),
	surface = theme_color("SURFACE"),
	on_surface = theme_color("ON_SURFACE"),
	surface_variant = theme_color("SURFACE_VARIANT"),
	on_surface_variant = theme_color("ON_SURFACE_VARIANT"),
	outline = theme_color("OUTLINE"),
	error = theme_color("ERROR"),
	on_error = theme_color("ON_ERROR"),
	selector_button = theme_color("SELECTOR_BUTTON"),
	secondary_text = theme_color("SECONDARY_TEXT"),
	toolbar_background = theme_color("TOOLBAR_BACKGROUND"),
	hyperlink = theme_color("HYPERLINK"),
	good = theme_color("GOOD"),
}

--- Track & Graph text styles, resolved by the app at render time.
--- Individual text properties supplied to `ui.text` override the selected style.
--- @since (API level 2)
M.text_style = {
	display_large = "TEXT_STYLE_DISPLAY_LARGE",
	display_medium = "TEXT_STYLE_DISPLAY_MEDIUM",
	display_small = "TEXT_STYLE_DISPLAY_SMALL",
	headline_large = "TEXT_STYLE_HEADLINE_LARGE",
	headline_medium = "TEXT_STYLE_HEADLINE_MEDIUM",
	headline_small = "TEXT_STYLE_HEADLINE_SMALL",
	title_large = "TEXT_STYLE_TITLE_LARGE",
	title_medium = "TEXT_STYLE_TITLE_MEDIUM",
	title_small = "TEXT_STYLE_TITLE_SMALL",
	body_large = "TEXT_STYLE_BODY_LARGE",
	body_medium = "TEXT_STYLE_BODY_MEDIUM",
	body_small = "TEXT_STYLE_BODY_SMALL",
	label_large = "TEXT_STYLE_LABEL_LARGE",
	label_medium = "TEXT_STYLE_LABEL_MEDIUM",
	label_small = "TEXT_STYLE_LABEL_SMALL",
	code = "TEXT_STYLE_CODE",
}

--- Track & Graph spacing tokens: 4dp, 5dp, 10dp, 20dp, and 50dp.
--- @since (API level 2)
M.spacing = {
	x_small = "SPACING_X_SMALL",
	small = "SPACING_SMALL",
	medium = "SPACING_MEDIUM",
	large = "SPACING_LARGE",
	x_large = "SPACING_X_LARGE",
}

--- Track & Graph shapes. Small, medium, and large currently use 4dp, 8dp, and 16dp rounded corners.
--- @since (API level 2)
M.shape = {
	small = "SHAPE_SMALL",
	medium = "SHAPE_MEDIUM",
	large = "SHAPE_LARGE",
	circle = "SHAPE_CIRCLE",
}

return M
