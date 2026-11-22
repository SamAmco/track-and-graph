-- Shared by the root sizing pass and the measured canvas drawing pass.
return function(width, density, font_scale)
	local tile = math.max(1, math.floor(14 * density))
	local step = tile + 1
	local label_width = math.ceil(34 * font_scale * density)
	local label_gap = math.ceil(6 * density)
	local weeks = math.max(0, math.floor((width - label_width - label_gap + 1) / step))
	if weeks == 0 then return nil end
	local header = math.ceil(36 * font_scale * density)
	return {
		tile = tile, step = step, label_width = label_width, label_gap = label_gap,
		weeks = weeks, header = header, height = header + 7 * step - 1,
	}
end
