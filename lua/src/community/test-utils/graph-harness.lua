-- Test-only convenience operations. All graph execution crosses the real-engine bridge.
return function(native)
    local harness = { descriptor = native.descriptor }
    local session = {}
    session.__index = session

    local function find(node, key)
        if node.key == key then return node end
        if node.child then
            local match = find(node.child, key)
            if match then return match end
        end
        for _, child in ipairs(node.children or {}) do
            local match = find(child, key)
            if match then return match end
        end
    end

    function session:find(key)
        return assert(find(self.ui, key), "No component with key: " .. key)
    end

    function session:render(params)
        params = params or {}
        self.context = params.context or self.context or { constraints = {} }
        if params.view_mode then self.additional.view_mode = params.view_mode end
        self.result = native.render(self.renderer, self.state, self.context, self.additional)
        self.ui = self.result.ui
        if self.result.state ~= nil then self.state = self.result.state end
        return self
    end

    function session:invoke(target, event, ...)
        local node = type(target) == "string" and self:find(target) or target
        local callback = node[event]
        if callback == nil and event == "on_click" then
            for _, modifier in ipairs(node.modifier or {}) do
                if modifier.type == "ON_CLICK" then callback = modifier.callback end
            end
        end
        assert(type(callback) == "function", "No handler: " .. event)
        native.call(callback, ...)
        return self:render()
    end

    function session:draw(key, context)
        return native.call(assert(self:find(key).draw, "Component has no draw callback"), context)
    end

    function session:save()
        return native.round_trip(self.state)
    end

    function harness:configure(params)
        params = params or {}
        return setmetatable({
            renderer = assert(self.descriptor.render_configuration, "No configuration renderer"),
            state = params.state,
            additional = { feature_map = params.feature_map or {} },
        }, session):render(params)
    end

    function harness:prepare(params)
        local result = native.prepare(params or {})
        result.additional = { data = result.data }
        return setmetatable(result, session)
    end

    function harness:call(callback, ...) return native.call(callback, ...) end
    return harness
end
