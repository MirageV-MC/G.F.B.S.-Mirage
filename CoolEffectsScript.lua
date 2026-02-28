local TweenService = game:GetService("TweenService")
local ServerStorage = game:GetService("ServerStorage")
local ServerScriptService = game:GetService("ServerScriptService")

local ShakeScript = require(ServerScriptService.Server.Game.Server:WaitForChild("shake"))

local CONFIG = {
	Paths = {
		Audios = game.SoundService,
		FacilityLights = workspace.Facility_Lights,
		Bulbs = workspace.Facility_Lights.Bulbs,
		GenLights = workspace.Facility_Lights.GenLights,
		Lights = workspace.Facility_Lights.Lights,
		Floodlights = workspace.Facility_Lights.Floodlights,
	},
	SoundIds = {
		Flicker = "rbxassetid://154904310",
		Breaker = "rbxassetid://154904310",
		Fan = "rbxassetid://341677804",
		FanOff = "rbxassetid://341678519",
		FanOn = "rbxassetid://341678379",
	},
	Colors = {
		AmbientOff = Color3.fromRGB(15, 15, 15),
		AmbientDim = Color3.fromRGB(25, 25, 25),
		AmbientOn = Color3.fromRGB(99, 99, 99),
	},
	Materials = {
		Neon = Enum.Material.Neon,
		SmoothPlastic = Enum.Material.SmoothPlastic,
	},
	Timing = {
		FlickerMin = 10,
		FlickerMax = 90,
		FlickerDivisor = 85,
		BreakerMin = 25,
		BreakerMax = 90,
		BreakerDivisor = 1000,
		RestoreMin = 25,
		RestoreMax = 90,
		RestoreDivisor = 100,
		TweenMin = 30,
		TweenMax = 60,
		TweenDivisor = 10,
	},
}

local Bulbs = {}
local Lights = {}
local Generic = {}
local GenericStat = {}
local Floodlights = {}

local module = {}

local function CreateSound(name, soundId, parent, volume, rollOffMaxDistance, playbackSpeed)
	local sound = Instance.new("Sound")
	sound.Name = name or "Sound"
	sound.SoundId = soundId
	sound.Volume = volume or 1
	sound.RollOffMaxDistance = rollOffMaxDistance or 200
	sound.RollOffMode = "Linear"
	if playbackSpeed then
		sound.PlaybackSpeed = playbackSpeed
	end
	sound.Parent = parent
	return sound
end

local function PlayFlickerSound()
	local parent = CONFIG.Paths.Audios.Lights
	if not parent then return end

	local soundId = parent:FindFirstChild("light_flicker_" .. math.random(1, 16))
	if not soundId then return end

	local sound = CreateSound("Flicker", soundId.SoundId, parent, 0.1, 10)
	sound:Play()
end

local function PlayBreakerSound(parent, volume)
	volume = volume or 0.75
	local playbackSpeed = math.random(100, 140) / 100
	local sound = CreateSound("Breaker", CONFIG.SoundIds.Breaker, parent, volume, 200, playbackSpeed)
	sound:Play()
	sound.Ended:Connect(function()
		sound:Destroy()
	end)
	return sound
end

local function FlickerLight(light, loopCount, minTime, maxTime, divisor, changeMaterial)
	for i = 1, loopCount do
		if changeMaterial and light.Parent then
			light.Parent.Material = CONFIG.Materials.SmoothPlastic
		end
		light.Enabled = false

		wait(math.random(minTime, maxTime) / divisor)

		if changeMaterial and light.Parent then
			light.Parent.Material = CONFIG.Materials.Neon
		end
		light.Enabled = true

		wait(math.random(minTime, maxTime) / divisor)
	end
end

local function IntenseFlickerLight(light, duration, changeMaterial)
	local startTime = tick()
	while tick() - startTime < duration do
		if changeMaterial and light.Parent then
			light.Parent.Material = CONFIG.Materials.SmoothPlastic
		end
		light.Enabled = false

		wait(math.random(5, 15) / 100)

		if changeMaterial and light.Parent then
			light.Parent.Material = CONFIG.Materials.Neon
		end
		light.Enabled = true

		wait(math.random(5, 15) / 100)
	end
end

local function ProcessLights(container, callback)
	if not container then return end

	for _, light in pairs(container:GetDescendants()) do
		if light:IsA("SpotLight") or light:IsA("PointLight") or light:IsA("SurfaceLight") then
			callback(light)
		end
	end
end

local function RestoreLightBrightness(light, brightnessTable, enabledTable)
	local brightness = brightnessTable[light]
	local enabled = enabledTable[light]

	if brightness then
		if enabled == nil or enabled == true then
			light.Enabled = true
			TweenService:Create(light, TweenInfo.new(math.random(CONFIG.Timing.TweenMin, CONFIG.Timing.TweenMax) / CONFIG.Timing.TweenDivisor), {Brightness = brightness}):Play()
		end
	end
end

local function StopSpecificSounds()
	for _, sound in pairs(workspace:GetDescendants()) do
		if sound:IsA("Sound") then
			if sound.SoundId == CONFIG.SoundIds.Fan then
				sound:Stop()
				if sound.Parent and sound.Parent:FindFirstChild("HingeConstraint") then
					sound.Parent.HingeConstraint.AngularVelocity = 0
				end
				if sound.Parent:FindFirstChild("Off") then
					sound.Parent.Off:Play()
				else
					local offSound = CreateSound("Off", CONFIG.SoundIds.FanOff, sound.Parent, 0.5)
					offSound:Play()
				end
			elseif sound.SoundId == "rbxassetid://3561264586" or sound.SoundId == "rbxassetid://3561263942" or sound.SoundId == "rbxassetid://143973639" or sound.SoundId == "rbxassetid://157204376" or (sound.SoundId == "rbxassetid://473495333" and sound.Name == "ambience1") then
				sound:Stop()
			elseif sound.SoundId == "rbxassetid://186721466" then
				sound:Stop()
				if sound.Parent and sound.Parent:FindFirstChild("SurfaceGui") then
					sound.Parent.SurfaceGui.Enabled = false
				end
			end
		end
	end
end

local function StartSpecificSounds()
	for _, sound in pairs(workspace:GetDescendants()) do
		if sound:IsA("Sound") then
			if sound.SoundId == CONFIG.SoundIds.Fan then
				sound:Play()
				if sound.Parent and sound.Parent:FindFirstChild("HingeConstraint") then
					sound.Parent.HingeConstraint.AngularVelocity = 28
				end
				if sound.Parent:FindFirstChild("On") then
					sound.Parent.On:Play()
				else
					local onSound = CreateSound("On", CONFIG.SoundIds.FanOn, sound.Parent, 0.5)
					onSound:Play()
				end
			elseif sound.SoundId == "rbxassetid://3561264586" or sound.SoundId == "rbxassetid://3561263942" or sound.SoundId == "rbxassetid://143973639" or sound.SoundId == "rbxassetid://157204376" or (sound.SoundId == "rbxassetid://473495333" and sound.Name == "ambience1") then
				sound:Play()
			elseif sound.SoundId == "rbxassetid://186721466" then
				sound:Play()
				if sound.Parent and sound.Parent:FindFirstChild("SurfaceGui") then
					sound.Parent.SurfaceGui.Enabled = true
				end
			end
		end
	end
end

local function ToggleFloodlights(enabled)
	ProcessLights(CONFIG.Paths.Floodlights, function(light)
		light.Enabled = enabled
		if light.Parent then
			light.Parent.Material = enabled and CONFIG.Materials.Neon or CONFIG.Materials.SmoothPlastic
		end
	end)
end

local function ActivateParticleEffects()
	for _, particle in pairs(CONFIG.Paths.FacilityLights:GetDescendants()) do
		if particle:IsA("ParticleEmitter") then
			coroutine.wrap(function()
				wait(1.75)
				particle.Enabled = true
				wait(math.random(50, 100) / 100)
				particle.Enabled = false
			end)()
		end
	end
end

local function FadeAmbientLight(color, duration)
	TweenService:Create(game:GetService("Lighting"), TweenInfo.new(duration), {Ambient = color}):Play()
end

module.Sound = PlayFlickerSound

module.Breaker2 = function()
	if CONFIG.Paths.Audios.Lights then
		PlayBreakerSound(CONFIG.Paths.Audios.Lights, 0.75)
	end
end

module.newbreaker = function()
	if CONFIG.Paths.Audios.Lights then
		PlayBreakerSound(CONFIG.Paths.Audios.Lights, 0.75)
	end
end

module.Flicker = function()
	ProcessLights(CONFIG.Paths.Bulbs, function(light)
		coroutine.wrap(function()
			IntenseFlickerLight(light, 0.75, true)
			FlickerLight(light, math.random(1, 3), CONFIG.Timing.FlickerMin, CONFIG.Timing.FlickerMax, CONFIG.Timing.FlickerDivisor, true)
		end)()
	end)

	ShakeScript.shake(100, 2)

	ProcessLights(CONFIG.Paths.GenLights, function(light)
		if light.Enabled then
			coroutine.wrap(function()
				IntenseFlickerLight(light, 0.75, true)
				FlickerLight(light, math.random(1, 3), CONFIG.Timing.FlickerMin, CONFIG.Timing.FlickerMax, CONFIG.Timing.FlickerDivisor, true)
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.Lights, function(light)
		coroutine.wrap(function()
			IntenseFlickerLight(light, 0.75, false)
			FlickerLight(light, math.random(1, 3), CONFIG.Timing.FlickerMin, CONFIG.Timing.FlickerMax, CONFIG.Timing.FlickerDivisor, false)
		end)()
	end)
end

module.Breaker = function()
	wait(1)
	if CONFIG.Paths.Audios.Lights.Sound then
		CONFIG.Paths.Audios.Lights.Sound:Play()
	end

	StopSpecificSounds()

	ProcessLights(CONFIG.Paths.Floodlights, function(light)
		Floodlights[light] = light.Brightness
	end)

	ProcessLights(CONFIG.Paths.GenLights, function(light)
		Generic[light] = light.Brightness
		GenericStat[light] = light.Enabled
	end)

	ProcessLights(CONFIG.Paths.Bulbs, function(light)
		Bulbs[light] = light.Brightness
	end)

	ProcessLights(CONFIG.Paths.Lights, function(light)
		Lights[light] = light.Brightness
	end)

	ToggleFloodlights(true)
	ActivateParticleEffects()

	ProcessLights(CONFIG.Paths.GenLights, function(light)
		if light.Enabled then
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.BreakerMin, CONFIG.Timing.BreakerMax, CONFIG.Timing.BreakerDivisor, true)
					PlayBreakerSound(light.Parent, 0.5)
				end

				if light.Parent then
					light.Parent.Material = CONFIG.Materials.SmoothPlastic
				end
				TweenService:Create(light, TweenInfo.new(math.random(CONFIG.Timing.TweenMin, CONFIG.Timing.TweenMax) / CONFIG.Timing.TweenDivisor), {Brightness = 0}):Play()
			end)()
		end
	end)

	wait(0.3)

	if CONFIG.Paths.Audios.Lights.Sound then
		CONFIG.Paths.Audios.Lights.Sound:Play()
	end

	ProcessLights(CONFIG.Paths.Bulbs, function(light)
		coroutine.wrap(function()
			local loopCount = math.random(2, 5)
			for i = 1, loopCount do
				FlickerLight(light, 1, CONFIG.Timing.BreakerMin, CONFIG.Timing.BreakerMax, CONFIG.Timing.BreakerDivisor, true)
				PlayBreakerSound(light.Parent, 0.5)
			end

			if light.Parent then
				light.Parent.Material = CONFIG.Materials.SmoothPlastic
			end
			TweenService:Create(light, TweenInfo.new(math.random(CONFIG.Timing.TweenMin, CONFIG.Timing.TweenMax) / CONFIG.Timing.TweenDivisor), {Brightness = 0}):Play()
		end)()
	end)

	wait(0.1)

	ProcessLights(CONFIG.Paths.Lights, function(light)
		coroutine.wrap(function()
			local loopCount = math.random(2, 5)
			for i = 1, loopCount do
				FlickerLight(light, 1, CONFIG.Timing.BreakerMin, CONFIG.Timing.BreakerMax, CONFIG.Timing.BreakerDivisor, false)
				PlayBreakerSound(light.Parent, 0.5)
			end

			TweenService:Create(light, TweenInfo.new(math.random(CONFIG.Timing.TweenMin, CONFIG.Timing.TweenMax) / CONFIG.Timing.TweenDivisor), {Brightness = 0}):Play()
		end)()
	end)

	FadeAmbientLight(CONFIG.Colors.AmbientOff, 0.1)
end

module.Off = function()
	if workspace.Audios.Effects.OtherUnworldlyNoises.insufficient then
		workspace.Audios.Effects.OtherUnworldlyNoises.insufficient:Play()
	end
	wait(0.9)

	StopSpecificSounds()

	ProcessLights(CONFIG.Paths.Floodlights, function(light)
		Floodlights[light] = light.Brightness
	end)

	ProcessLights(CONFIG.Paths.GenLights, function(light)
		Generic[light] = light.Brightness
		GenericStat[light] = light.Enabled
	end)

	ProcessLights(CONFIG.Paths.Bulbs, function(light)
		Bulbs[light] = light.Brightness
	end)

	ProcessLights(CONFIG.Paths.Lights, function(light)
		Lights[light] = light.Brightness
	end)

	ProcessLights(CONFIG.Paths.Floodlights, function(light)
		coroutine.wrap(function()
			local loopCount = math.random(2, 5)
			for i = 1, loopCount do
				FlickerLight(light, 1, CONFIG.Timing.BreakerMin, CONFIG.Timing.BreakerMax, CONFIG.Timing.BreakerDivisor, true)
			end

			if light.Parent then
				light.Parent.Material = CONFIG.Materials.SmoothPlastic
			end
		end)()
	end)

	ToggleFloodlights(true)
	ActivateParticleEffects()

	ProcessLights(CONFIG.Paths.GenLights, function(light)
		if light.Enabled then
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.BreakerMin, CONFIG.Timing.BreakerMax, CONFIG.Timing.BreakerDivisor, true)
				end

				if light.Parent then
					light.Parent.Material = CONFIG.Materials.SmoothPlastic
				end
				TweenService:Create(light, TweenInfo.new(math.random(CONFIG.Timing.TweenMin, CONFIG.Timing.TweenMax) / CONFIG.Timing.TweenDivisor), {Brightness = 0}):Play()
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.Bulbs, function(light)
		coroutine.wrap(function()
			local loopCount = math.random(2, 5)
			for i = 1, loopCount do
				FlickerLight(light, 1, CONFIG.Timing.BreakerMin, CONFIG.Timing.BreakerMax, CONFIG.Timing.BreakerDivisor, true)
			end

			if light.Parent then
				light.Parent.Material = CONFIG.Materials.SmoothPlastic
			end
			TweenService:Create(light, TweenInfo.new(math.random(CONFIG.Timing.TweenMin, CONFIG.Timing.TweenMax) / CONFIG.Timing.TweenDivisor), {Brightness = 0}):Play()
		end)()
	end)

	ProcessLights(CONFIG.Paths.Lights, function(light)
		coroutine.wrap(function()
			local loopCount = math.random(2, 5)
			for i = 1, loopCount do
				FlickerLight(light, 1, CONFIG.Timing.BreakerMin, CONFIG.Timing.BreakerMax, CONFIG.Timing.BreakerDivisor, false)
			end

			TweenService:Create(light, TweenInfo.new(math.random(CONFIG.Timing.TweenMin, CONFIG.Timing.TweenMax) / CONFIG.Timing.TweenDivisor), {Brightness = 0}):Play()
		end)()
	end)

	wait(6)
	FadeAmbientLight(CONFIG.Colors.AmbientDim, 3.5)
	wait(4)
end

module.InstaOn = function()
	StartSpecificSounds()

	ProcessLights(CONFIG.Paths.Floodlights, function(light)
		if Floodlights[light] then
			RestoreLightBrightness(light, Floodlights, {})
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, true)
				end
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.GenLights, function(light)
		if GenericStat[light] == true and Generic[light] then
			light.Brightness = Generic[light]
			if light.Parent then
				light.Parent.Material = CONFIG.Materials.Neon
			end
			light.Enabled = true
		end
	end)

	ProcessLights(CONFIG.Paths.Bulbs, function(light)
		if Bulbs[light] then
			light.Brightness = Bulbs[light]
			if light.Parent then
				light.Parent.Material = CONFIG.Materials.Neon
			end
			light.Enabled = true
		end
	end)

	ProcessLights(CONFIG.Paths.Lights, function(light)
		if Lights[light] then
			light.Brightness = Lights[light]
			light.Enabled = true
		end
	end)

	Bulbs = {}
	Lights = {}
	Generic = {}
	GenericStat = {}
	Floodlights = {}
end

module.On = function()
	wait(0.9)

	StartSpecificSounds()

	ProcessLights(CONFIG.Paths.Floodlights, function(light)
		if Floodlights[light] then
			RestoreLightBrightness(light, Floodlights, {})
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, true)
				end
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.GenLights, function(light)
		if GenericStat[light] == true then
			RestoreLightBrightness(light, Generic, GenericStat)
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, true)
				end
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.Bulbs, function(light)
		if Bulbs[light] then
			RestoreLightBrightness(light, Bulbs, {})
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, true)
				end
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.Lights, function(light)
		if Lights[light] then
			RestoreLightBrightness(light, Lights, {})
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, false)
				end
			end)()
		end
	end)

	wait(3)
	FadeAmbientLight(CONFIG.Colors.AmbientOn, 6)

	wait(9)
	Bulbs = {}
	Lights = {}
	Generic = {}
	GenericStat = {}
	Floodlights = {}
end

module.LightsOn = function()
	wait(0.9)

	StartSpecificSounds()

	ProcessLights(CONFIG.Paths.Floodlights, function(light)
		if Floodlights[light] then
			RestoreLightBrightness(light, Floodlights, {})
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, true)
					if light.Parent then
						PlayBreakerSound(light.Parent, 0.5)
					end
				end
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.GenLights, function(light)
		if GenericStat[light] == true then
			RestoreLightBrightness(light, Generic, GenericStat)
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, true)
					if light.Parent then
						PlayBreakerSound(light.Parent, 0.5)
					end
				end
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.Bulbs, function(light)
		if Bulbs[light] then
			RestoreLightBrightness(light, Bulbs, {})
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, true)
					if light.Parent then
						PlayBreakerSound(light.Parent, 0.5)
					end
				end
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.Lights, function(light)
		if Lights[light] then
			RestoreLightBrightness(light, Lights, {})
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, false)
					if light.Parent then
						PlayBreakerSound(light.Parent, 0.5)
					end
				end
			end)()
		end
	end)

	wait(3)
	FadeAmbientLight(CONFIG.Colors.AmbientOn, 6)

	wait(9)
	Bulbs = {}
	Lights = {}
	Generic = {}
	GenericStat = {}
	Floodlights = {}
end

module.StartOn = function()
	StartSpecificSounds()

	ProcessLights(CONFIG.Paths.Floodlights, function(light)
		if Floodlights[light] then
			RestoreLightBrightness(light, Floodlights, {})
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, true)
				end
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.GenLights, function(light)
		if GenericStat[light] == true then
			RestoreLightBrightness(light, Generic, GenericStat)
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, true)
				end
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.Bulbs, function(light)
		if Bulbs[light] then
			RestoreLightBrightness(light, Bulbs, {})
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, true)
				end
			end)()
		end
	end)

	ProcessLights(CONFIG.Paths.Lights, function(light)
		if Lights[light] then
			RestoreLightBrightness(light, Lights, {})
			coroutine.wrap(function()
				local loopCount = math.random(2, 5)
				for i = 1, loopCount do
					FlickerLight(light, 1, CONFIG.Timing.RestoreMin, CONFIG.Timing.RestoreMax, CONFIG.Timing.RestoreDivisor, false)
				end
			end)()
		end
	end)

	wait(3)
	FadeAmbientLight(CONFIG.Colors.AmbientOn, 6)

	wait(9)
	Bulbs = {}
	Lights = {}
	Generic = {}
	GenericStat = {}
end

script.Parent.on.Event:Connect(module.On)
script.Parent.off.Event:Connect(module.Breaker)
script.Parent.EngergyOff.Event:Connect(module.Off)
script.Parent.EngergyOn.Event:Connect(module.On)
script.Parent.Event.Event:Connect(module.LightsOn)

return module
