-- API specification for core.lua module
-- Maps each exported symbol to the API level where it was introduced
-- All existing APIs are level 1
return {
	-- Functions
	time = 1,
	date = 1,
	shift = 1,
	format = 1,
	get_cutoff = 1,
	get_end_of_period = 1,

	-- Enums
	COLOR = 1,
	["COLOR.RED_DARK"] = 1,
	["COLOR.RED"] = 1,
	["COLOR.ORANGE_DARK"] = 1,
	["COLOR.ORANGE"] = 1,
	["COLOR.YELLOW"] = 1,
	["COLOR.BLUE_LIGHT"] = 1,
	["COLOR.BLUE_SKY"] = 1,
	["COLOR.BLUE"] = 1,
	["COLOR.BLUE_DARK"] = 1,
	["COLOR.BLUE_NAVY"] = 1,
	["COLOR.GREEN_LIGHT"] = 1,
	["COLOR.GREEN_DARK"] = 1,

	DURATION = 1,
	["DURATION.SECOND"] = 1,
	["DURATION.MINUTE"] = 1,
	["DURATION.HOUR"] = 1,
	["DURATION.DAY"] = 1,
	["DURATION.WEEK"] = 1,

	PERIOD = 1,
	["PERIOD.DAY"] = 1,
	["PERIOD.WEEK"] = 1,
	["PERIOD.MONTH"] = 1,
	["PERIOD.YEAR"] = 1,

	-- Type stubs
	Timestamp = 1,
	Date = 1,
	DataPoint = 1,
	CutoffParams = 1,

	-- DataSource class and methods
	DataSource = 1,
	["DataSource.dp"] = 1,
	["DataSource.dpbatch"] = 1,
	["DataSource.dpall"] = 1,
	["DataSource.dpafter"] = 1,
}
