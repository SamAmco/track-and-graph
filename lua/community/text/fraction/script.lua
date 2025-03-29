local core = require("tng.core")
local graph = require("tng.graph")

--- PREVIEW_START
-- Script: Text - Fraction
-- Optional period of data to be displayed e.g. core.PERIOD.WEEK to show data for this week
local period = nil
-- Optional integer value used with period e.g. 5
local period_multiplier = nil
-- Optional boolean to count by label. If true, each datapoint counts as 1, and the value is ignored
local count_by_label = false
-- Optional fixed denominator value e.g. 100
local fixed_denominator = nil
-- The list of labels to be counted as part of the numerator
local numerator_labels = {}
-- A list of labels to be ignored i.e. not counted as part of the numerator or denominator
local ignored_labels = nil
--- PREVIEW_END

local function contains(tab, val)
	for _, value in ipairs(tab) do
		if value == val then
			return true
		end
	end
	return false
end

-- Function to calculate the fraction based on data points
local function calculate_fraction(data_points)
	if not data_points or #data_points == 0 then
		return nil
	end

	local numerator = 0
	local denominator = 0

	-- Process data points
	for _, point in ipairs(data_points) do
		local label = point.label
		local value = count_by_label and 1 or point.value or 1

		-- Skip ignored labels
		if not ignored_labels or not contains(ignored_labels, label) then
			if contains(numerator_labels, label) then
				numerator = numerator + value
			end
			denominator = denominator + value
		end
	end

	if denominator == 0 then
		return nil
	end

	-- Use fixed denominator if specified
	if fixed_denominator then
		-- Scale the numerator to maintain the correct proportion
		local scaling_factor = fixed_denominator / denominator
		numerator = numerator * scaling_factor
		denominator = fixed_denominator
	end

	return numerator .. "/" .. denominator
end

return function(sources)
	local all_data_points = {}

	local cutoff_params = {
		period = period,
		period_multiplier = period_multiplier,
	}
	local cutoff = period and core.get_cutoff(cutoff_params)

	for _, source in pairs(sources) do
		local data_points
		if cutoff then
			data_points = source.dpafter(cutoff)
		else
			data_points = source.dpall()
		end
		for _, point in ipairs(data_points) do
			table.insert(all_data_points, point)
		end
	end

	local fraction_text = calculate_fraction(all_data_points)

	return fraction_text and {
		type = graph.GRAPH_TYPE.TEXT,
		text = fraction_text,
	}
end
