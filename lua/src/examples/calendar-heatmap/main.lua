local graph = require("tng.graph")
local data = require("data")
local configuration = require("configuration")
local renderer = require("renderer")
local strings = require("strings")

return {
	id = "calendar-heatmap",
	version = "1.0.0",
	title = strings.title,
	purpose = graph.TYPE,
	type = graph.GRAPH_TYPE.CUSTOM,
	prepare_data = data,
	render_graph = renderer,
	render_configuration = configuration,
}
