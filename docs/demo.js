const palette = {
  green: [40, 210, 110],
  blue: [53, 143, 243],
  purple: [150, 110, 255],
  pink: [240, 90, 190],
  orange: [255, 150, 50],
  grey: [92, 92, 100],
}

const bayer = [
  [0, 8, 2, 10],
  [12, 4, 14, 6],
  [3, 11, 1, 9],
  [15, 7, 13, 5],
].map((row) => row.map((value) => (value + 0.5) / 16))

const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug"]
const desktop = [186, 240, 205, 278, 255, 322, 298, 352]
const mobile = [80, 118, 142, 165, 188, 215, 234, 252]
const state = { variant: "gradient", bloom: "aura", stack: "default", hover: {} }

function rgb(color, alpha = 1) {
  return `rgba(${color[0]},${color[1]},${color[2]},${alpha})`
}

function fit(canvas) {
  const ratio = Math.min(window.devicePixelRatio || 1, 2)
  const rect = canvas.getBoundingClientRect()
  const width = Math.max(1, Math.round(rect.width * ratio))
  const height = Math.max(1, Math.round(rect.height * ratio))
  if (canvas.width !== width || canvas.height !== height) {
    canvas.width = width
    canvas.height = height
  }
  const context = canvas.getContext("2d")
  context.setTransform(ratio, 0, 0, ratio, 0, 0)
  return { context, width: rect.width, height: rect.height }
}

function densityFor(variant, depth) {
  if (variant === "solid") return 1
  if (variant === "dotted") return 0.5
  if (variant === "hatched") return 0.75
  return 0.22 + 0.78 * depth
}

function cellVisible(x, y, density, variant) {
  if (variant === "solid") return true
  if (variant === "hatched" && ((x + y) & 3) >= 2) return false
  return density > bayer[y & 3][x & 3] - (variant === "dotted" ? 0.12 : 0)
}

function glow(context, color) {
  const presets = { off: 0, low: 8, aura: 22 }
  context.shadowColor = rgb(color, state.bloom === "aura" ? 0.8 : 0.5)
  context.shadowBlur = presets[state.bloom]
}

function linePoints(values, box, max = 380) {
  return values.map((value, index) => ({
    x: box.left + (box.width * index) / (values.length - 1),
    y: box.top + box.height * (1 - value / max),
  }))
}

function sampleLine(points, x) {
  const clamped = Math.max(points[0].x, Math.min(points.at(-1).x, x))
  const span = points.at(-1).x - points[0].x
  const position = ((clamped - points[0].x) / span) * (points.length - 1)
  const low = Math.floor(position)
  const high = Math.min(points.length - 1, low + 1)
  const amount = position - low
  return points[low].y + (points[high].y - points[low].y) * amount
}

function drawGrid(context, box, labels = true) {
  context.save()
  context.strokeStyle = "rgba(90,90,103,.27)"
  context.lineWidth = 1
  context.setLineDash([3, 4])
  for (let index = 0; index < 5; index += 1) {
    const y = box.top + (box.height * index) / 4
    context.beginPath()
    context.moveTo(box.left, y)
    context.lineTo(box.left + box.width, y)
    context.stroke()
  }
  context.setLineDash([])
  if (labels) {
    context.fillStyle = "#6f6f7a"
    context.font = "9px SFMono-Regular, Consolas, monospace"
    months.forEach((month, index) => {
      const x = box.left + (box.width * index) / (months.length - 1)
      context.fillText(month, x - 9, box.top + box.height + 18)
    })
  }
  context.restore()
}

function fillBand(context, top, bottom, color, cell = 2) {
  context.save()
  glow(context, color)
  const columns = Math.floor((top.at(-1).x - top[0].x) / cell)
  for (let column = 0; column <= columns; column += 1) {
    const x = top[0].x + column * cell
    const y1 = sampleLine(top, x)
    const y2 = typeof bottom === "number" ? bottom : sampleLine(bottom, x)
    const start = Math.floor(Math.min(y1, y2) / cell)
    const end = Math.ceil(Math.max(y1, y2) / cell)
    for (let row = start; row < end; row += 1) {
      const depth = (row - start) / Math.max(1, end - start)
      const density = densityFor(state.variant, depth)
      if (!cellVisible(column, row, density, state.variant)) continue
      context.fillStyle = rgb(color, 0.35 + density * 0.55)
      context.fillRect(x, row * cell, cell + 0.2, cell + 0.2)
    }
  }
  context.shadowBlur = 0
  context.strokeStyle = rgb(color, 0.82)
  context.lineWidth = 1.3
  context.beginPath()
  top.forEach((point, index) => index ? context.lineTo(point.x, point.y) : context.moveTo(point.x, point.y))
  context.stroke()
  context.restore()
}

function marker(context, box, index, series) {
  if (index == null) return
  const x = box.left + (box.width * index) / (months.length - 1)
  context.save()
  context.strokeStyle = "rgba(220,220,230,.26)"
  context.beginPath()
  context.moveTo(x, box.top)
  context.lineTo(x, box.top + box.height)
  context.stroke()
  series.forEach(({ points, color }) => {
    const y = points[index].y
    context.fillStyle = "#101013"
    context.strokeStyle = rgb(color)
    context.lineWidth = 2
    context.beginPath()
    context.arc(x, y, 3.5, 0, Math.PI * 2)
    context.fill()
    context.stroke()
  })
  context.restore()
}

function chartBox(width, height) {
  return { left: 40, top: 18, width: width - 58, height: height - 54 }
}

function drawArea(card) {
  const canvas = card.querySelector("canvas")
  const { context, width, height } = fit(canvas)
  context.clearRect(0, 0, width, height)
  const box = chartBox(width, height)
  drawGrid(context, box)
  const first = linePoints(desktop, box)
  const second = linePoints(mobile, box)
  fillBand(context, first, box.top + box.height, palette.blue)
  fillBand(context, second, second.map((point) => ({ ...point, y: point.y + 30 })), palette.purple)
  marker(context, box, state.hover.area, [{ points: first, color: palette.blue }, { points: second, color: palette.purple }])
}

function drawLine(card) {
  const canvas = card.querySelector("canvas")
  const { context, width, height } = fit(canvas)
  context.clearRect(0, 0, width, height)
  const box = chartBox(width, height)
  drawGrid(context, box)
  const points = linePoints(desktop, box)
  fillBand(context, points, points.map((point) => ({ ...point, y: point.y + 22 })), palette.pink)
  const target = box.top + box.height * (1 - 200 / 380)
  context.strokeStyle = "rgba(160,160,170,.55)"
  context.setLineDash([5, 5])
  context.beginPath()
  context.moveTo(box.left, target)
  context.lineTo(box.left + box.width, target)
  context.stroke()
  context.setLineDash([])
  context.fillStyle = "#8c8c98"
  context.font = "9px SFMono-Regular, Consolas, monospace"
  context.fillText("Target", box.left + box.width - 38, target - 5)
  marker(context, box, state.hover.line, [{ points, color: palette.pink }])
}

function drawBar(card) {
  const canvas = card.querySelector("canvas")
  const { context, width, height } = fit(canvas)
  context.clearRect(0, 0, width, height)
  const box = chartBox(width, height)
  drawGrid(context, box)
  const max = state.stack === "stacked" ? 580 : state.stack === "percent" ? 1 : 380
  const slot = box.width / months.length
  const cell = 2
  const series = [desktop, mobile]
  const colors = [palette.green, palette.orange]
  months.forEach((_, index) => {
    let base = 0
    const total = desktop[index] + mobile[index]
    series.forEach((values, seriesIndex) => {
      const value = state.stack === "percent" ? values[index] / total : values[index]
      const start = state.stack === "default" ? 0 : base
      const end = start + value
      if (state.stack !== "default") base = end
      const barWidth = state.stack === "default" ? slot * 0.34 : slot * 0.62
      const x = box.left + index * slot + (state.stack === "default" ? slot * (0.14 + seriesIndex * 0.38) : slot * 0.19)
      const top = box.top + box.height * (1 - end / max)
      const bottom = box.top + box.height * (1 - start / max)
      glow(context, colors[seriesIndex])
      for (let px = 0; px < barWidth; px += cell) {
        for (let py = top; py < bottom; py += cell) {
          const density = densityFor(state.variant, (py - top) / Math.max(1, bottom - top))
          if (!cellVisible(Math.floor(px / cell), Math.floor(py / cell), density, state.variant)) continue
          context.fillStyle = rgb(colors[seriesIndex], 0.35 + density * 0.55)
          context.fillRect(x + px, py, cell, cell)
        }
      }
      context.shadowBlur = 0
    })
  })
}

function drawPie(card) {
  const canvas = card.querySelector("canvas")
  const { context, width, height } = fit(canvas)
  context.clearRect(0, 0, width, height)
  const values = [275, 200, 145, 90]
  const colors = [palette.blue, palette.green, palette.orange, palette.purple]
  const total = values.reduce((sum, value) => sum + value, 0)
  const center = { x: width / 2, y: height / 2 }
  const outer = Math.min(width, height) * 0.37
  const inner = outer * 0.5
  const cell = 2
  let starts = []
  let angle = -Math.PI / 2
  values.forEach((value) => { starts.push(angle); angle += (value / total) * Math.PI * 2 })
  for (let y = center.y - outer; y <= center.y + outer; y += cell) {
    for (let x = center.x - outer; x <= center.x + outer; x += cell) {
      const dx = x - center.x
      const dy = y - center.y
      const radius = Math.hypot(dx, dy)
      if (radius < inner || radius > outer) continue
      let pointAngle = Math.atan2(dy, dx)
      if (pointAngle < -Math.PI / 2) pointAngle += Math.PI * 2
      let index = values.length - 1
      for (let candidate = 0; candidate < starts.length; candidate += 1) {
        const end = starts[candidate] + (values[candidate] / total) * Math.PI * 2
        if (pointAngle >= starts[candidate] && pointAngle <= end) { index = candidate; break }
      }
      const density = densityFor(state.variant, (radius - inner) / (outer - inner))
      if (!cellVisible(Math.floor(x / cell), Math.floor(y / cell), density, state.variant)) continue
      context.fillStyle = rgb(colors[index], 0.4 + density * 0.55)
      glow(context, colors[index])
      context.fillRect(x, y, cell, cell)
    }
  }
  context.shadowBlur = 0
  context.fillStyle = "#f4f4f5"
  context.font = "600 24px SFMono-Regular, Consolas, monospace"
  context.textAlign = "center"
  context.fillText("710", center.x, center.y - 2)
  context.fillStyle = "#777782"
  context.font = "9px SFMono-Regular, Consolas, monospace"
  context.fillText("VISITORS", center.x, center.y + 17)
  context.textAlign = "start"
}

function drawRadar(card) {
  const canvas = card.querySelector("canvas")
  const { context, width, height } = fit(canvas)
  context.clearRect(0, 0, width, height)
  const center = { x: width / 2, y: height / 2 + 6 }
  const radius = Math.min(width, height) * 0.34
  const labels = ["Speed", "Power", "Range", "Control", "Focus"]
  const sets = [[.92, .76, .84, .68, .89], [.74, .88, .58, .82, .70]]
  const colors = [palette.blue, palette.pink]
  const angles = labels.map((_, index) => -Math.PI / 2 + (index * Math.PI * 2) / labels.length)
  context.strokeStyle = "rgba(80,80,92,.48)"
  context.lineWidth = 1
  for (let level = 1; level <= 4; level += 1) {
    context.beginPath()
    angles.forEach((angle, index) => {
      const x = center.x + Math.cos(angle) * radius * level / 4
      const y = center.y + Math.sin(angle) * radius * level / 4
      index ? context.lineTo(x, y) : context.moveTo(x, y)
    })
    context.closePath()
    context.stroke()
  }
  context.fillStyle = "#777782"
  context.font = "9px SFMono-Regular, Consolas, monospace"
  labels.forEach((label, index) => {
    const angle = angles[index]
    context.beginPath()
    context.moveTo(center.x, center.y)
    context.lineTo(center.x + Math.cos(angle) * radius, center.y + Math.sin(angle) * radius)
    context.stroke()
    context.fillText(label, center.x + Math.cos(angle) * (radius + 12) - 16, center.y + Math.sin(angle) * (radius + 12))
  })
  sets.forEach((values, setIndex) => {
    const points = values.map((value, index) => ({ x: center.x + Math.cos(angles[index]) * radius * value, y: center.y + Math.sin(angles[index]) * radius * value }))
    const cell = 2
    const minX = Math.min(...points.map((point) => point.x))
    const maxX = Math.max(...points.map((point) => point.x))
    const minY = Math.min(...points.map((point) => point.y))
    const maxY = Math.max(...points.map((point) => point.y))
    for (let y = minY; y <= maxY; y += cell) {
      for (let x = minX; x <= maxX; x += cell) {
        if (!insidePolygon(x, y, points)) continue
        const density = densityFor(state.variant, 0.55)
        if (!cellVisible(Math.floor(x / cell), Math.floor(y / cell), density, state.variant)) continue
        context.fillStyle = rgb(colors[setIndex], setIndex ? 0.48 : 0.62)
        glow(context, colors[setIndex])
        context.fillRect(x, y, cell, cell)
      }
    }
    context.shadowBlur = 0
    context.strokeStyle = rgb(colors[setIndex], .9)
    context.beginPath()
    points.forEach((point, index) => index ? context.lineTo(point.x, point.y) : context.moveTo(point.x, point.y))
    context.closePath()
    context.stroke()
  })
}

function insidePolygon(x, y, points) {
  let inside = false
  for (let index = 0, previous = points.length - 1; index < points.length; previous = index++) {
    const a = points[index]
    const b = points[previous]
    if ((a.y > y) !== (b.y > y) && x < ((b.x - a.x) * (y - a.y)) / (b.y - a.y) + a.x) inside = !inside
  }
  return inside
}

function hash(value) {
  let result = 0x811c9dc5
  for (let index = 0; index < value.length; index += 1) {
    result ^= value.charCodeAt(index)
    result = Math.imul(result, 0x01000193)
  }
  return result >>> 0
}

function random(seed) {
  let value = seed || 0x9e3779b9
  return () => {
    value ^= value << 13
    value >>>= 0
    value ^= value >>> 17
    value ^= value << 5
    value >>>= 0
    return value / 0x100000000
  }
}

function drawAvatar(canvas, name) {
  canvas.width = 32
  canvas.height = 32
  const context = canvas.getContext("2d")
  const next = random(hash(name))
  const bits = Array.from({ length: 32 }, () => next() < 0.5)
  const vertical = next() < 0.5
  const hue = Math.floor(next() * 180) * 2
  const densities = Array.from({ length: 32 }, () => 0.55 + next() * 0.45)
  const color = hsl(hue)
  for (let row = 0; row < 8; row += 1) {
    for (let column = 0; column < 8; column += 1) {
      const index = vertical ? Math.min(row, 7 - row) * 8 + column : row * 4 + Math.min(column, 7 - column)
      if (!bits[index]) continue
      for (let py = 0; py < 4; py += 1) {
        for (let px = 0; px < 4; px += 1) {
          const density = densities[index]
          const lit = density > bayer[py][px]
          context.fillStyle = rgb(color, lit ? 0.35 + density * 0.65 : density * 0.25)
          context.fillRect(column * 4 + px, row * 4 + py, 1, 1)
        }
      }
    }
  }
}

function hsl(hue) {
  const h = ((hue % 360) + 360) % 360
  const chroma = 0.714
  const x = chroma * (1 - Math.abs((h / 60) % 2 - 1))
  const values = h < 60 ? [chroma, x, 0] : h < 120 ? [x, chroma, 0] : h < 180 ? [0, chroma, x] : h < 240 ? [0, x, chroma] : h < 300 ? [x, 0, chroma] : [chroma, 0, x]
  return values.map((value) => Math.round((value + 0.223) * 255))
}

function drawGradient(canvas) {
  const { context, width, height } = fit(canvas)
  context.clearRect(0, 0, width, height)
  const cell = 3
  for (let y = 0; y < height; y += cell) {
    for (let x = 0; x < width; x += cell) {
      const amount = x / width
      const lit = 1 - amount > bayer[Math.floor(y / cell) & 3][Math.floor(x / cell) & 3]
      context.fillStyle = rgb(lit ? palette.purple : palette.blue, 0.85)
      context.fillRect(x, y, cell, cell)
    }
  }
}

function drawSpark(canvas) {
  const { context, width, height } = fit(canvas)
  context.clearRect(0, 0, width, height)
  const box = { left: 4, top: 10, width: width - 8, height: height - 18 }
  const points = linePoints([3, 7, 5, 9, 8, 12, 11, 15], box, 16)
  fillBand(context, points, box.top + box.height, palette.green)
}

function drawAll() {
  document.querySelectorAll("[data-demo]").forEach((card) => {
    const name = card.dataset.demo
    if (name === "area") drawArea(card)
    if (name === "line") drawLine(card)
    if (name === "bar") drawBar(card)
    if (name === "pie") drawPie(card)
    if (name === "radar") drawRadar(card)
  })
  drawGradient(document.querySelector(".gradient-demo canvas"))
  drawSpark(document.querySelector(".spark-demo canvas"))
}

function bindControls() {
  document.querySelectorAll("[data-control]").forEach((group) => {
    group.addEventListener("click", (event) => {
      const button = event.target.closest("button")
      if (!button) return
      group.querySelectorAll("button").forEach((item) => item.classList.toggle("active", item === button))
      state[group.dataset.control] = button.dataset.value
      drawAll()
    })
  })
}

function bindCharts() {
  document.querySelectorAll("[data-demo=area], [data-demo=line]").forEach((card) => {
    const canvas = card.querySelector("canvas")
    const tooltip = card.querySelector(".tooltip")
    canvas.addEventListener("pointermove", (event) => {
      const rect = canvas.getBoundingClientRect()
      const index = Math.max(0, Math.min(months.length - 1, Math.round(((event.clientX - rect.left - 40) / (rect.width - 58)) * (months.length - 1))))
      state.hover[card.dataset.demo] = index
      const values = card.dataset.demo === "area" ? `Desktop&nbsp; ${desktop[index]}<br>Mobile&nbsp;&nbsp;&nbsp; ${mobile[index]}` : `Desktop&nbsp; ${desktop[index]}`
      tooltip.innerHTML = `<strong>${months[index]}</strong><br>${values}`
      tooltip.style.left = `${event.clientX - rect.left}px`
      tooltip.style.top = `${event.clientY - rect.top}px`
      tooltip.classList.add("visible")
      card.dataset.demo === "area" ? drawArea(card) : drawLine(card)
    })
    canvas.addEventListener("pointerleave", () => {
      state.hover[card.dataset.demo] = null
      tooltip.classList.remove("visible")
      card.dataset.demo === "area" ? drawArea(card) : drawLine(card)
    })
  })
}

function bindActions() {
  document.querySelectorAll(".preview-actions").forEach((actions) => {
    const demo = actions.closest(".demo")
    const preview = actions.querySelector(".code-toggle:not(.code-button)")
    const code = actions.querySelector(".code-button")
    preview.addEventListener("click", () => setCodeVisible(demo, preview, code, false))
    code.addEventListener("click", () => setCodeVisible(demo, preview, code, true))
  })
  document.querySelectorAll(".replay").forEach((button) => {
    button.addEventListener("click", () => {
      const canvas = button.closest(".demo").querySelector("canvas")
      drawAll()
      canvas.animate([{ opacity: 0.15, transform: "translateY(3px)" }, { opacity: 1, transform: "translateY(0)" }], { duration: 420, easing: "ease-out" })
    })
  })
  const save = document.querySelector(".dither-button")
  save.addEventListener("click", () => {
    const status = document.querySelector(".button-status")
    status.textContent = "saved ✓"
    window.setTimeout(() => { status.textContent = "ready" }, 1300)
  })
  document.querySelectorAll(".copy-command").forEach((button) => {
    button.addEventListener("click", async () => {
      await navigator.clipboard.writeText(button.dataset.copy)
      const icon = button.querySelector("span")
      icon.textContent = "✓"
      window.setTimeout(() => { icon.textContent = "⧉" }, 1300)
    })
  })
  const inlineCopy = document.querySelector(".copy-inline")
  inlineCopy.addEventListener("click", async () => {
    await navigator.clipboard.writeText(inlineCopy.previousElementSibling.textContent)
    inlineCopy.textContent = "✓"
    window.setTimeout(() => { inlineCopy.textContent = "⧉" }, 1300)
  })

  const root = document.querySelector(".dk-docs")
  const theme = document.querySelector(".theme-toggle")
  theme.addEventListener("click", () => {
    const light = root.classList.toggle("light")
    root.classList.toggle("dark", !light)
    theme.textContent = light ? "☾" : "☼"
    theme.setAttribute("aria-label", light ? "Switch to dark theme" : "Switch to light theme")
    document.querySelector('meta[name="theme-color"]').content = light ? "#ffffff" : "#0d0d0f"
    drawAll()
  })

  const dial = document.querySelector(".dial-button")
  const panel = document.querySelector(".control-panel")
  dial.addEventListener("click", () => {
    const open = panel.hidden
    panel.hidden = !open
    dial.setAttribute("aria-expanded", String(open))
    dial.setAttribute("aria-label", open ? "Close chart controls" : "Open chart controls")
  })

  const installCommands = document.querySelectorAll(".copy-command")
  document.querySelectorAll("[data-install]").forEach((button) => {
    button.addEventListener("click", () => {
      document.querySelectorAll("[data-install]").forEach((item) => item.classList.toggle("active", item === button))
      const checkout = button.dataset.install === "checkout"
      const values = checkout
        ? [
            "include(\":dither-kit-compose\")",
            "implementation(project(\":dither-kit-compose\"))",
          ]
        : [
            "./gradlew :dither-kit-compose:publishToMavenLocal",
            'implementation("io.github.ryumacodes:dither-kit-compose:0.1.0-SNAPSHOT")',
          ]
      installCommands.forEach((command, index) => {
        command.dataset.copy = values[index]
        command.querySelector("code").textContent = `${index === 0 && !checkout ? "$ " : ""}${values[index]}`
      })
    })
  })
}

function setCodeVisible(demo, preview, code, visible) {
  demo.classList.toggle("show-code", visible)
  preview.classList.toggle("active", !visible)
  code.classList.toggle("active", visible)
  preview.setAttribute("aria-expanded", String(!visible))
  code.setAttribute("aria-expanded", String(visible))
}

const avatarNames = ["ryuma", "dither-kit", "compose", "tripwire"]

document.querySelectorAll(".avatar-row").forEach((row) => {
  avatarNames.forEach((name) => {
    const canvas = document.createElement("canvas")
    canvas.setAttribute("aria-label", `${name} avatar`)
    drawAvatar(canvas, name)
    row.append(canvas)
  })
})

bindControls()
bindCharts()
bindActions()
drawAll()
window.addEventListener("resize", drawAll)
