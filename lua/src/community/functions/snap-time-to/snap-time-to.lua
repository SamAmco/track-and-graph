-- Lua Function to snap data point timestamps to a specific time of day
-- This function adjusts timestamps to the specified time of day based on the direction (next, previous, or nearest)

local localtime = require("tng.config").localtime
local enum = require("tng.config").enum
local core = require("tng.core")

return {
  -- Configuration metadata
  id = "snap-time-to",
  version = "1.0.0",
  inputCount = 1,
  categories = { "_time" },

  title = {
    ["en"] = "Snap Time To",
    ["de"] = "Zeit Einrasten Auf",
    ["es"] = "Ajustar Tiempo A",
    ["fr"] = "Aligner l'Heure Sur",
  },

  description = {
    ["en"] = [[
Snaps data point timestamps to a specific time of day.

- Time of Day: The target time (e.g., 09:30:00)
- Direction: Next, Previous, or Nearest occurrence of that time
]],
    ["de"] = [[
Rastet Datenpunkt-Zeitstempel auf eine bestimmte Tageszeit ein.

- Tageszeit: Die Zielzeit (z.B. 09:30:00)
- Richtung: Nächste, Vorherige oder Nächstgelegene Occurrence dieser Zeit
]],
    ["es"] = [[
Ajusta las marcas de tiempo de los puntos de datos a una hora específica del día.

- Hora del Día: La hora objetivo (ej. 09:30:00)
- Dirección: Siguiente, Anterior, o Más Cercana ocurrencia de esa hora
]],
    ["fr"] = [[
Aligne les horodatages des points de données sur une heure spécifique de la journée.

- Heure du Jour: L'heure cible (ex. 09:30:00)
- Direction: Prochaine, Précédente, ou Plus Proche occurrence de cette heure
]],
  },

  config = {
    localtime {
      id = "target_time",
      name = {
        ["en"] = "Time of Day",
        ["de"] = "Tageszeit",
        ["es"] = "Hora del Día",
        ["fr"] = "Heure du Jour",
      },
      default = 9 * core.DURATION.HOUR, -- 09:00:00
    },
    enum {
      id = "direction",
      name = {
        ["en"] = "Direction",
        ["de"] = "Richtung",
        ["es"] = "Dirección",
        ["fr"] = "Direction",
      },
      options = { "_next", "_previous", "_nearest" },
      default = "_nearest",
    },
  },

  -- Generator function
  generator = function(source, config)
    local target_time = config and config.target_time or error("Target time is required")
    local direction = config and config.direction or error("Direction is required")

    return function()
      local data_point = source.dp()
      if not data_point then
        return nil
      end

      -- Get the date components of the data point
      local date = core.date(data_point)

      -- Calculate the target time on the same date
      local same_day_target = core.time({
        year = date.year,
        month = date.month,
        day = date.day,
        hour = 0,
        min = 0,
        sec = 0,
        zone = date.zone
      })
      same_day_target = core.shift(same_day_target, target_time)

      local new_timestamp

      if direction == "_next" then
        -- Find next occurrence of target time
        if data_point.timestamp <= same_day_target.timestamp then
          new_timestamp = same_day_target
        else
          -- Next day
          new_timestamp = core.shift(same_day_target, core.PERIOD.DAY)
        end
      elseif direction == "_previous" then
        -- Find previous occurrence of target time
        if data_point.timestamp >= same_day_target.timestamp then
          new_timestamp = same_day_target
        else
          -- Previous day
          new_timestamp = core.shift(same_day_target, core.PERIOD.DAY, -1)
        end
      else -- "_nearest"
        -- Find nearest occurrence of target time
        local other_target
        if data_point.timestamp <= same_day_target.timestamp then
          other_target = core.shift(same_day_target, core.PERIOD.DAY, -1)
        else
          other_target = core.shift(same_day_target, core.PERIOD.DAY)
        end

        local diff_same = math.abs(data_point.timestamp - same_day_target.timestamp)
        local diff_other = math.abs(data_point.timestamp - other_target.timestamp)

        if diff_same <= diff_other then
          new_timestamp = same_day_target
        else
          new_timestamp = other_target
        end
      end

      -- Return data point with adjusted timestamp
      return {
        timestamp = new_timestamp.timestamp,
        offset = new_timestamp.offset,
        value = data_point.value,
        label = data_point.label,
        note = data_point.note,
      }
    end
  end,
}
