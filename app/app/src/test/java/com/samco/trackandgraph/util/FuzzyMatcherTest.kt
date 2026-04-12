package com.samco.trackandgraph.util

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {

    private fun score(query: String, target: String) = FuzzyMatcher.score(query, target)

    // ── Basic match / no-match ─────────────────────────────────────────────────

    @Test
    fun `empty query always matches with score zero`() {
        val s = score("", "anything")
        assertNotNull(s)
        assertTrue(s!! == 0.0)
    }

    @Test
    fun `empty target returns null`() {
        assertNull(score("a", ""))
    }

    @Test
    fun `query longer than target returns null`() {
        assertNull(score("abcd", "abc"))
    }

    @Test
    fun `single char match`() {
        assertNotNull(score("a", "a"))
    }

    @Test
    fun `single char no match`() {
        assertNull(score("z", "abc"))
    }

    @Test
    fun `all chars present in order is a match`() {
        assertNotNull(score("abc", "xaxbxcx"))
    }

    @Test
    fun `chars present but not in order is not a match`() {
        // 'c' appears at index 2, 'b' at 1, 'a' at 0 - impossible to satisfy "cba" in order
        assertNull(score("cba", "abc"))
    }

    @Test
    fun `case insensitive - lowercase query uppercase target`() {
        assertNotNull(score("abc", "ABC"))
    }

    @Test
    fun `case insensitive - uppercase query lowercase target`() {
        assertNotNull(score("ABC", "abc"))
    }

    // ── Ranking: exact > prefix > consecutive > boundary > scattered ──────────

    @Test
    fun `exact match scores higher than substring`() {
        val exact = score("abc", "abc")!!
        val sub = score("abc", "abcdef")!!
        assertTrue("exact=$exact should beat sub=$sub", exact > sub)
    }

    @Test
    fun `exact match scores higher than scattered match`() {
        val exact = score("abc", "abc")!!
        val scattered = score("abc", "xaxbxcx")!!
        assertTrue("exact=$exact should beat scattered=$scattered", exact > scattered)
    }

    @Test
    fun `prefix match scores higher than non-prefix match`() {
        val prefix = score("ab", "abcdef")!!
        val nonPrefix = score("ab", "xabcdef")!!
        assertTrue("prefix=$prefix should beat nonPrefix=$nonPrefix", prefix > nonPrefix)
    }

    @Test
    fun `consecutive run scores higher than scattered`() {
        val consecutive = score("abc", "abcXXX")!!
        val scattered = score("abc", "aXbXcX")!!
        assertTrue("consecutive=$consecutive should beat scattered=$scattered", consecutive > scattered)
    }

    @Test
    fun `dp picks best alignment - trailing consecutive beats leading scattered`() {
        // "aXbXcXabc": greedy first-match gives scattered (a=0,b=2,c=4); DP should prefer
        // the trailing consecutive run (a=7, b=8, c=9)
        val preferConsecutive = score("abc", "aXbXcXabc")!!
        val scattered = score("abc", "aXbXcX")!!
        // The consecutive suffix should pull the score close to a pure consecutive match
        assertTrue(
            "score in mixed string ($preferConsecutive) should be greater than scattered ($scattered)",
            preferConsecutive > scattered
        )
    }

    @Test
    fun `word boundary match scores higher than mid-word match`() {
        // 'h' and 'r' both land on word boundaries in "Heart Rate"
        val boundary = score("hr", "Heart Rate")!!
        val midWord = score("hr", "xhxrx")!!
        assertTrue("boundary=$boundary should beat midWord=$midWord", boundary > midWord)
    }

    @Test
    fun `camelCase boundary is recognised`() {
        val camel = score("hr", "HeartRate")!!
        val midWord = score("hr", "thxr")!!
        assertTrue("camel=$camel should beat midWord=$midWord", camel > midWord)
    }

    @Test
    fun `underscore boundary is recognised`() {
        val boundary = score("hr", "heart_rate")!!
        val midWord = score("hr", "thxr")!!
        assertTrue("boundary=$boundary should beat midWord=$midWord", boundary > midWord)
    }

    // ── Case-sensitivity bonuses ───────────────────────────────────────────────

    @Test
    fun `case-exact match scores higher than case-insensitive match`() {
        // "abc" in "abc" gets BONUS_CASE_EXACT; "ABC" in "abc" does not
        val caseExact = score("abc", "abc")!!
        val caseFlipped = score("ABC", "abc")!!
        assertTrue("caseExact=$caseExact should beat caseFlipped=$caseFlipped", caseExact > caseFlipped)
    }

    @Test
    fun `short exact match scores higher than short match inside longer target`() {
        // "A" should match "A" better than it matches "Abc" (BONUS_EXACT_MATCH fires)
        val exactShort = score("A", "A")!!
        val partialLong = score("A", "Abc")!!
        assertTrue("exactShort=$exactShort should beat partialLong=$partialLong", exactShort > partialLong)
    }
}
