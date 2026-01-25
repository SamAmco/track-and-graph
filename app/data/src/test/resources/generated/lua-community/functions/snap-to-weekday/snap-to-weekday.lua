-- Lua Function to snap data point timestamps to the same local time on a specific weekday
-- This function adjusts timestamps to the same local time but on the specified weekday based on the direction (next, last, or nearest)

local enum = require("tng.config").enum
local core = require("tng.core")

return {
  -- Configuration metadata
  id = "snap-to-weekday",
  version = "1.0.0",
  inputCount = 1,
  categories = { "_time" },

  title = {
    ["en"] = "Snap To Weekday",
    ["de"] = "Auf Wochentag Einrasten",
    ["es"] = "Ajustar A Día De La Semana",
    ["fr"] = "Aligner Sur Jour De La Semaine",
  },

  description = {
    ["en"] = [[
Snaps data point timestamps to the same local time on a specific weekday.

- Weekday: The target day of the week (Monday through Sunday)
- Direction: Last, Nearest, or Next occurrence of that local time on that weekday

The data point keeps its original time of day but moves to the specified weekday.
]],
    ["de"] = [[
Rastet Datenpunkt-Zeitstempel auf die gleiche Ortszeit an einem bestimmten Wochentag ein.

- Wochentag: Der Ziel-Wochentag (Montag bis Sonntag)
- Richtung: Letzte, Nächstgelegene oder Nächste Occurrence dieser Ortszeit an diesem Wochentag

Der Datenpunkt behält seine ursprüngliche Tageszeit bei, wird aber auf den angegebenen Wochentag verschoben.
]],
    ["es"] = [[
Ajusta las marcas de tiempo de los puntos de datos a la misma hora local en un día específico de la semana.

- Día de la Semana: El día objetivo de la semana (Lunes a Domingo)
- Dirección: Última, Más Cercana, o Siguiente ocurrencia de esa hora local en ese día de la semana

El punto de datos mantiene su hora original del día pero se mueve al día de la semana especificado.
]],
    ["fr"] = [[
Aligne les horodatages des points de données sur la même heure locale d'un jour spécifique de la semaine.

- Jour de la Semaine: Le jour cible de la semaine (Lundi à Dimanche)
- Direction: Dernière, Plus Proche, ou Prochaine occurrence de cette heure locale ce jour de la semaine

Le point de données conserve son heure d'origine mais se déplace vers le jour de la semaine spécifié.
]],
  },

  config = {
    enum {
      id = "target_weekday",
      name = {
        ["en"] = "Weekday",
        ["de"] = "Wochentag",
        ["es"] = "Día de la Semana",
        ["fr"] = "Jour de la Semaine",
      },
      options = { "_monday", "_tuesday", "_wednesday", "_thursday", "_friday", "_saturday", "_sunday" },
      default = "_monday",
    },
    enum {
      id = "direction",
      name = {
        ["en"] = "Direction",
        ["de"] = "Richtung",
        ["es"] = "Dirección",
        ["fr"] = "Direction",
      },
      options = { "_next", "_nearest", "_last" },
      default = "_nearest",
    },
  },

  -- Generator function
  generator = function(source, config)
    local target_weekday = config and config.target_weekday or error("target_weekday is required")
    local direction = config and config.direction or error("direction is required")

    -- Map weekday strings to numbers (Monday = 1, Sunday = 7)
    local weekday_map = {
      ["_monday"] = 1,
      ["_tuesday"] = 2,
      ["_wednesday"] = 3,
      ["_thursday"] = 4,
      ["_friday"] = 5,
      ["_saturday"] = 6,
      ["_sunday"] = 7,
    }
    local target_wday = weekday_map[target_weekday]
    if not target_wday then
      error("Invalid weekday: " .. target_weekday)
    end

    return function()
      local data_point = source.dp()
      if not data_point then
        return nil
      end

      -- Get the date components of the data point
      local date = core.date(data_point)
      local current_wday = date.wday

      -- Calculate days difference to target weekday
      local days_to_target = (target_wday - current_wday) % 7

      -- Calculate the target time on the target weekday in the same week
      -- Use the original time components from the data point
      local next_target = core.shift(data_point, core.PERIOD.DAY, days_to_target)

      local new_timestamp

      if days_to_target == 0 then
        -- Already on target weekday, no change needed
        new_timestamp = data_point
      elseif direction == "_next" then
        new_timestamp = next_target
      elseif direction == "_last" then
        new_timestamp = core.shift(next_target, core.PERIOD.WEEK, -1)
      else -- "_nearest"
        -- Find nearest occurrence of same time on target weekday
        local last_target = core.shift(next_target, core.PERIOD.WEEK, -1)
        local next_diff = math.abs(next_target.timestamp - data_point.timestamp)
        local last_diff = math.abs(data_point.timestamp - last_target.timestamp)

        if next_diff < last_diff then
          new_timestamp = next_target
        else
          new_timestamp = last_target
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
