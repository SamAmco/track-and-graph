-- Pack whole months into rows, sharing the week that straddles each boundary.
return function(months, width, density, font_scale, show_weekdays)
	local gutter = show_weekdays and math.ceil(40 * density * font_scale) or 0
	local tile = math.max(1, math.floor(14 * density * font_scale))
	if width > 0 then tile = math.max(1, math.min(tile, math.floor((width - gutter + 1) / 6) - 1)) end
	local step = tile + 1
	local rows = {}
	local row = { months = {}, width = gutter, gutter = gutter }
	for _, month in ipairs(months) do
		local weeks = month.days[#month.days].week
		local month_width = weeks * step - 1
		local x = row.gutter
		if #row.months > 0 then
			-- On both sides of a midweek boundary, adjacent tiles are five pixels apart.
			-- Their week columns overlap horizontally, but their weekdays never overlap.
			x = row.width + 5 - (month.days[1].weekday == 1 and 0 or step)
		end
		if #row.months > 0 and x + month_width > width then
			rows[#rows + 1] = row
			row = { months = {}, width = 0, gutter = 0 }
			x = 0
		end
		row.months[#row.months + 1] = { month = month, x = x }
		row.width = x + month_width
	end
	if #row.months > 0 then rows[#rows + 1] = row end
	local header = math.ceil(24 * density * font_scale)
	return { rows = rows, tile = tile, step = step, header = header, height = header + 7 * step - 1 }
end
