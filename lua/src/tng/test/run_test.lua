#!/usr/bin/env lua
-- Runs one test file and always finalizes the shared test counters.

local file_path = assert(arg[1], "Test file path is required")

dofile(file_path)

-- Most existing suites finalize themselves. This fallback makes omitted
-- finish_tests() calls fail the process instead of silently passing.
require("src.tng.test.test_helpers").finish_tests()
