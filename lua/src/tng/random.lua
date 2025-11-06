local M = {}

-- Although Lua has built-in math.random and math.randomseed functions, the stability of those
-- generated numbers across different Lua versions and platforms is not guaranteed.
-- Also the current Lua engine implementation supports only a single 32 bit seed, which
-- can be tricky to work with if you are trying to seed from an epoch milliseconds timestamp
-- (as is common in TnG)
--
-- Therefore, we provide our own Random class for stable performant random number generation
-- based on the Xoroshiro128+ algorithm.
--
-- One added benefit is that multiple Random instances can be created with different seeds,
-- allowing for independent random number streams.

--- @since (API level 2)
--- @class Random
M.Random = {}

--- Returns the next random number in the sequence.
--- @since (API level 2)
--- @param min number?: The minimum value (inclusive) of the random number. Defaults to 0.0.
--- @param max number?: The maximum value (exclusive) of the random number. Defaults to 1.0.
--- @return number: A double in the range [min, max).
function M.Random:next(min, max) end

--- Creates a new Random object seeded with the given values.
--- @since (API level 2)
--- @param a (number)?: The first seed value. Can be a number or
--- @param b (number)?: The second seed value. Can be a number or nil.
--- @return Random
M.new_seeded_random = function(a, b) end

return M
