-- You probably need these imports before the config because they may need to be references. e.g. core.PERIOD.WEEK
-- Track & Graph will render everything before the first blank line in the preview, so keep both the requires and config above the first blank line.
local core = require("tng.core")
local graph = require("tng.graph")
-- All config goes here. 
local config_parameter = nil

-- You receive the sources as a table of datasources
-- The keys are the names of the datasources, and the values are the datasource objects which contain the methods defined in tng.graph e.g. source.dp(), source.dpbatch(count), dpall() etc.
return function(sources)
	-- You can use the sources to fetch data points from the datasources

	-- At the end return a graph object to be rendered by Track & Graph
	return {
		type = graph.GRAPH_TYPE.TEXT,
		text = "Hello, World!"
	}
end
