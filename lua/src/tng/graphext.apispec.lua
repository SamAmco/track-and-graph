-- API specification for graphext.lua module
-- Maps each exported symbol to the API level where it was introduced
-- All existing APIs are level 1
return {
	apply_moving_averaging = 1,
	calculate_period_totals = 1,
	find_latest_data_point = 1,
	merge_sources = 1,
	collect_to_bars = 1,
	collect_to_segments = 1,
}
