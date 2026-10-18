local graph = require("tng.graph")
local data = require("data")
local renderer = require("renderer")

return {
	id = "canvas-basic",
	version = "2.0.0",
	title = {
		en = "Basic canvas graph",
	},
	purpose = graph.TYPE,
	type = graph.GRAPH_TYPE.CUSTOM,
	data = data,
	render = renderer,
}
