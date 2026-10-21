local graph = require("tng.graph")
local data = require("data")
local renderer = require("renderer")
local strings = require("strings")

return {
	id = "canvas-basic",
	version = "2.0.0",
	title = strings.title,
	purpose = graph.TYPE,
	type = graph.GRAPH_TYPE.CUSTOM,
	prepare_data = data,
	render_graph = renderer,
}
