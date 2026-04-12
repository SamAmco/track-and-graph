/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.util

/**
 * Ranked fuzzy string matcher inspired by fzf.
 *
 * A query matches a target if all query characters appear in the target in order (subsequence
 * match). Characters may be separated by any number of other characters. The returned score
 * reflects match quality — higher is better. Returns null when there is no match.
 *
 * Scoring rewards:
 *  - Consecutive character runs (+[BONUS_CONSECUTIVE] per char after the first in a run)
 *  - Word-boundary alignment (+[BONUS_WORD_BOUNDARY] when a matched char opens a word)
 *  - Prefix match (+[BONUS_PREFIX] when the first query char lands at position 0)
 *  - Exact match (+[BONUS_EXACT_MATCH] when query equals target, case-insensitive and trimmed)
 *  - Case-exact containment (+[BONUS_CASE_EXACT] when target contains query as-is)
 *
 * The algorithm uses dynamic programming to find the highest-scoring alignment rather than the
 * first found, so "abc" in "aXbXcXabc" correctly prefers the trailing consecutive run.
 */
object FuzzyMatcher {

    private const val SCORE_CHAR_MATCH = 10.0
    private const val BONUS_CONSECUTIVE = 15.0
    private const val BONUS_WORD_BOUNDARY = 10.0
    private const val BONUS_PREFIX = 20.0
    private const val BONUS_EXACT_MATCH = 50.0
    private const val BONUS_CASE_EXACT = 10.0

    /**
     * Returns a match-quality score, or null if [query] is not a subsequence of [target].
     *
     * Matching is case-insensitive. An empty query always matches with score 0.
     */
    fun score(query: String, target: String): Double? {
        if (query.isEmpty()) return 0.0
        if (target.isEmpty()) return null

        val queryLow = query.lowercase()
        val targetLow = target.lowercase()
        val tLen = targetLow.length
        val qLen = queryLow.length

        if (qLen > tLen) return null

        val negInf = Double.NEGATIVE_INFINITY
        // dp[i][j] = best total score for matching q[0..j] with q[j] aligned to t[i]
        val dp = Array(tLen) { DoubleArray(qLen) { negInf } }

        // Base case: first query character
        for (i in 0 until tLen) {
            if (targetLow[i] != queryLow[0]) continue
            var score = SCORE_CHAR_MATCH
            if (i == 0) score += BONUS_PREFIX
            if (isWordBoundary(target, i)) score += BONUS_WORD_BOUNDARY
            dp[i][0] = score
        }

        // Fill DP for remaining query characters
        for (j in 1 until qLen) {
            // i must be at least j so there is room for j characters before it
            for (i in j until tLen) {
                if (targetLow[i] != queryLow[j]) continue

                var charScore = SCORE_CHAR_MATCH
                if (isWordBoundary(target, i)) charScore += BONUS_WORD_BOUNDARY

                // Find the best-scoring previous alignment at some k < i for q[j-1]
                var bestPrev = negInf
                for (k in (j - 1) until i) {
                    if (dp[k][j - 1] == negInf) continue
                    val consecutive = if (i == k + 1) BONUS_CONSECUTIVE else 0.0
                    val candidate = dp[k][j - 1] + consecutive
                    if (candidate > bestPrev) bestPrev = candidate
                }

                if (bestPrev != negInf) dp[i][j] = bestPrev + charScore
            }
        }

        // Best total score over all valid complete alignments
        var best = negInf
        for (i in (qLen - 1) until tLen) {
            if (dp[i][qLen - 1] > best) best = dp[i][qLen - 1]
        }

        if (best == negInf) return null

        // Post-alignment bonuses
        if (targetLow.trim() == queryLow.trim()) best += BONUS_EXACT_MATCH
        if (target.contains(query)) best += BONUS_CASE_EXACT

        return best
    }

    /**
     * Returns true if position [i] in [target] is a word boundary: start of string, a character
     * after a space/dash/underscore, or a camelCase uppercase-after-lowercase transition.
     */
    private fun isWordBoundary(target: String, i: Int): Boolean {
        if (i == 0) return true
        val prev = target[i - 1]
        val curr = target[i]
        return prev == ' ' || prev == '-' || prev == '_' ||
                (prev.isLowerCase() && curr.isUpperCase())
    }
}
