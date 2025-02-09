Tng = {}

--- Data Point Structure:
-- @field timestamp number: The epoch millisecond timestamp of the data point.
-- @field featureId string: The database ID of the feature (aka data source).
-- @field value number: The value of the data point.
-- @field label string: The label of the data point.
-- @field note string: The note of the data point.

--- Fetches the next group of data points from the data source.
-- @param name string: The name of the data source.
-- @param count number: The number of data points to retrieve.
-- @return table: A table containing the requested data points.
Tng.graph.nextBatch = function(name, count) end

--- Fetches the next data point from the data source.
-- @param name string: The name of the data source.
-- @return a data point table.
Tng.graph.next = function(name) end

--- Supported graph types.
---
-- datapoint
-- examples:
-- {
--    "type": "datapoint",
--    "generator": function()
--        return Tng.graph.next("data-source-name")
--    end
--    "isDuration": false -- optional
-- }
--
-- You can also just return a data point table directly but you need a valid featureId
-- {
--    "type": "datapoint",
--    "generator": function()
--        return {
--            "timestamp": 1614556800000,
--            "featureId": 1234,
--            "value": 1.0,
--            "label": "label",
--            "note": "note"
--        }
--    end
--    "isDuration": false -- optional
-- }

return Tng
