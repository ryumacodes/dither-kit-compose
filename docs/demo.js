const palette = {
  green: [40, 210, 110],
  blue: [53, 143, 243],
  purple: [150, 110, 255],
  pink: [240, 90, 190],
  orange: [255, 150, 50],
  red: [246, 78, 92],
  grey: [92, 92, 100],
}

const bayer = [
  [0, 8, 2, 10],
  [12, 4, 14, 6],
  [3, 11, 1, 9],
  [15, 7, 13, 5],
].map((row) => row.map((value) => (value + 0.5) / 16))

const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug"]
const desktop = [120, 190, 230, 235, 200, 155, 120, 125]
const mobile = [115, 125, 120, 115, 105, 95, 75, 80]
const defaults = {
  desktopVariant: "gradient",
  mobileVariant: "gradient",
  bloom: "aura",
  stacked: true,
  pieInnerRadius: 0.5,
  entranceMs: 900,
}
const state = { ...defaults, hover: {} }

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
    context.font = "10px 'Geist Mono', SFMono-Regular, Consolas, monospace"
    months.forEach((month, index) => {
      const x = box.left + (box.width * index) / (months.length - 1)
      context.fillText(month, x - 9, box.top + box.height + 18)
    })
    ;[300, 200, 100, 0].forEach((value, index) => {
      const y = box.top + (box.height * index) / 3
      context.fillText(String(value), box.left - 35, y + 3)
    })
  }
  context.restore()
}

function fillBand(context, top, bottom, color, cell = 2, variant = state.desktopVariant) {
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
      const density = densityFor(variant, depth)
      if (!cellVisible(column, row, density, variant)) continue
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
  return { left: 50, top: 18, width: width - 68, height: height - 54 }
}

function drawArea(card) {
  const canvas = card.querySelector("canvas")
  const { context, width, height } = fit(canvas)
  context.clearRect(0, 0, width, height)
  const box = chartBox(width, height)
  drawGrid(context, box)
  const first = linePoints(desktop, box)
  const second = linePoints(desktop.map((value, index) => value + mobile[index]), box)
  fillBand(context, first, box.top + box.height, palette.blue, 2, state.desktopVariant)
  fillBand(context, second, first, palette.purple, 2, state.mobileVariant)
  marker(context, box, state.hover.area, [{ points: first, color: palette.blue }, { points: second, color: palette.purple }])
}

function drawLine(card) {
  const canvas = card.querySelector("canvas")
  const { context, width, height } = fit(canvas)
  context.clearRect(0, 0, width, height)
  const box = chartBox(width, height)
  drawGrid(context, box)
  const points = linePoints(desktop, box, 280)
  const mobilePoints = linePoints([110, 108, 100, 88, 74, 62, 60, 68], box, 280)
  fillBand(context, points, points.map((point) => ({ ...point, y: point.y + 34 })), palette.blue, 2, state.desktopVariant)
  fillBand(context, mobilePoints, mobilePoints.map((point) => ({ ...point, y: point.y + 34 })), palette.purple, 2, state.mobileVariant)
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
  marker(context, box, state.hover.line, [{ points, color: palette.blue }, { points: mobilePoints, color: palette.purple }])
}

function drawBar(card) {
  const canvas = card.querySelector("canvas")
  const { context, width, height } = fit(canvas)
  context.clearRect(0, 0, width, height)
  const box = chartBox(width, height)
  drawGrid(context, box)
  const max = 380
  const slot = box.width / months.length
  const cell = 2
  const series = [desktop, mobile]
  const colors = [palette.blue, palette.purple]
  months.forEach((_, index) => {
    let base = 0
    series.forEach((values, seriesIndex) => {
      const value = values[index]
      const start = state.stacked ? base : 0
      const end = start + value
      if (state.stacked) base = end
      const barWidth = state.stacked ? slot * 0.62 : slot * 0.34
      const x = box.left + index * slot + (state.stacked ? slot * 0.19 : slot * (0.14 + seriesIndex * 0.38))
      const top = box.top + box.height * (1 - end / max)
      const bottom = box.top + box.height * (1 - start / max)
      glow(context, colors[seriesIndex])
      for (let px = 0; px < barWidth; px += cell) {
        for (let py = top; py < bottom; py += cell) {
          const variant = seriesIndex === 0 ? state.desktopVariant : state.mobileVariant
          const density = densityFor(variant, (py - top) / Math.max(1, bottom - top))
          if (!cellVisible(Math.floor(px / cell), Math.floor(py / cell), density, variant)) continue
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
  const values = [275, 200, 145, 90, 55]
  const colors = [palette.blue, palette.green, palette.orange, palette.purple, palette.grey]
  const total = values.reduce((sum, value) => sum + value, 0)
  const center = { x: width / 2, y: height / 2 }
  const outer = Math.min(width, height) * 0.37
  const inner = outer * state.pieInnerRadius
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
      const variant = index % 2 === 0 ? state.desktopVariant : state.mobileVariant
      const density = densityFor(variant, (radius - inner) / Math.max(1, outer - inner))
      if (!cellVisible(Math.floor(x / cell), Math.floor(y / cell), density, variant)) continue
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
  const labels = ["Speed", "Power", "Range", "Defense", "Magic", "Luck"]
  const sets = [[.92, .92, .70, .84, .72, .74], [.60, .55, .68, .65, .88, .58]]
  const colors = [palette.blue, palette.purple]
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
        const variant = setIndex === 0 ? state.desktopVariant : state.mobileVariant
        const density = densityFor(variant, 0.55)
        if (!cellVisible(Math.floor(x / cell), Math.floor(y / cell), density, variant)) continue
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
  const demo = canvas.closest(".gradient-demo")
  const direction = demo.dataset.direction || "down"
  const from = palette[demo.dataset.from] || palette.orange
  const to = palette[demo.dataset.to] || null
  const cell = 3
  for (let y = 0; y < height; y += cell) {
    for (let x = 0; x < width; x += cell) {
      const progress = direction === "up" ? 1 - y / height : direction === "left" ? 1 - x / width : direction === "right" ? x / width : y / height
      const density = to ? 1 : 1 - progress
      const lit = density > bayer[Math.floor(y / cell) & 3][Math.floor(x / cell) & 3]
      if (!lit) continue
      const color = to ? from.map((value, index) => Math.round(value + (to[index] - value) * progress)) : from
      context.fillStyle = rgb(color, to ? 0.9 : 0.35 + density * 0.65)
      context.fillRect(x, y, cell, cell)
    }
  }
}

function drawSpark(canvas) {
  const { context, width, height } = fit(canvas)
  context.clearRect(0, 0, width, height)
  const box = { left: 4, top: 10, width: width - 8, height: height - 18 }
  const points = linePoints([3, 7, 5, 9, 8, 12, 11, 15], box, 16)
  fillBand(context, points, box.top + box.height, palette.green, 2, state.desktopVariant)
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
}

function bindControls() {
  document.querySelectorAll("select[data-setting]").forEach((select) => {
    select.addEventListener("change", () => {
      state[select.dataset.setting] = select.value
      drawAll()
    })
  })

  document.querySelectorAll("[data-stack]").forEach((button) => {
    button.addEventListener("click", () => {
      state.stacked = button.dataset.stack === "true"
      document.querySelectorAll("[data-stack]").forEach((item) => item.classList.toggle("active", item === button))
      drawAll()
    })
  })

  document.querySelectorAll('input[type="range"][data-setting]').forEach((input) => {
    input.addEventListener("input", () => {
      const setting = input.dataset.setting
      state[setting] = Number(input.value)
      syncRange(input)
      if (setting === "pieInnerRadius") drawAll()
    })
    syncRange(input)
  })

  document.querySelector(".reset-controls").addEventListener("click", () => {
    Object.assign(state, defaults)
    syncControlPanel()
    drawAll()
  })

  document.querySelector(".replay-all").addEventListener("click", () => {
    drawAll()
    animateCanvases(document.querySelectorAll("canvas"))
  })
}

function syncRange(input) {
  const progress = ((Number(input.value) - Number(input.min)) / (Number(input.max) - Number(input.min))) * 100
  input.style.setProperty("--range-progress", `${progress}%`)
  input.nextElementSibling.value = input.dataset.setting === "pieInnerRadius" ? Number(input.value).toFixed(2) : input.value
}

function syncControlPanel() {
  document.querySelector('[data-setting="desktopVariant"]').value = state.desktopVariant
  document.querySelector('[data-setting="mobileVariant"]').value = state.mobileVariant
  document.querySelectorAll("[data-stack]").forEach((button) => button.classList.toggle("active", (button.dataset.stack === "true") === state.stacked))
  document.querySelectorAll('input[type="range"][data-setting]').forEach((input) => {
    input.value = state[input.dataset.setting]
    syncRange(input)
  })
}

const revealedCanvases = new WeakSet()

function revealFrames(canvas) {
  if (canvas.closest("[data-demo=pie], [data-demo=radar], .avatar-demo")) {
    return [
      { opacity: 0, clipPath: "circle(0% at 50% 50%)", filter: "brightness(.55)" },
      { opacity: 1, offset: 0.18 },
      { opacity: 1, clipPath: "circle(75% at 50% 50%)", filter: "brightness(1)" },
    ]
  }
  if (canvas.closest(".gradient-demo")) {
    return [
      { opacity: 0, clipPath: "inset(0 0 100% 0)", filter: "brightness(.55)" },
      { opacity: 1, offset: 0.18 },
      { opacity: 1, clipPath: "inset(0 0 0% 0)", filter: "brightness(1)" },
    ]
  }
  return [
    { opacity: 0, clipPath: "inset(0 100% 0 0)", filter: "brightness(.55)" },
    { opacity: 1, offset: 0.14 },
    { opacity: 1, clipPath: "inset(0 0% 0 0)", filter: "brightness(1)" },
  ]
}

function animateCanvases(canvases, stagger = 0) {
  ;[...canvases].forEach((canvas, index) => {
    revealedCanvases.add(canvas)
    canvas.style.opacity = "1"
    canvas.style.clipPath = ""
    canvas.getAnimations().forEach((animation) => animation.cancel())
    canvas.animate(revealFrames(canvas), {
      duration: state.entranceMs,
      delay: index * stagger,
      easing: "cubic-bezier(.2,.8,.2,1)",
      fill: "both",
    })
  })
}

function setupEntranceAnimations() {
  const canvases = document.querySelectorAll(".demo canvas, .avatar-demo canvas, .gradient-demo canvas")
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches || !("IntersectionObserver" in window)) {
    canvases.forEach((canvas) => { canvas.style.opacity = "1" })
    return
  }

  canvases.forEach((canvas) => {
    canvas.classList.add("reveal-canvas")
    canvas.style.opacity = "0"
  })

  const observer = new IntersectionObserver((entries) => {
    const entering = entries.filter((entry) => entry.isIntersecting && !revealedCanvases.has(entry.target))
    if (!entering.length) return
    animateCanvases(entering.map((entry) => entry.target), 35)
    entering.forEach((entry) => observer.unobserve(entry.target))
  }, { threshold: 0.18, rootMargin: "0px 0px -8% 0px" })

  canvases.forEach((canvas) => observer.observe(canvas))
}

function bindCharts() {
  document.querySelectorAll("[data-demo=area], [data-demo=line]").forEach((card) => {
    const canvas = card.querySelector("canvas")
    const tooltip = card.querySelector(".tooltip")
    canvas.addEventListener("pointermove", (event) => {
      const rect = canvas.getBoundingClientRect()
      const index = Math.max(0, Math.min(months.length - 1, Math.round(((event.clientX - rect.left - 50) / (rect.width - 68)) * (months.length - 1))))
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
  document.querySelectorAll(".demo > .demo-heading .preview-actions").forEach((actions) => {
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
      animateCanvases([canvas])
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
    root.classList.toggle("panel-open", open)
    dial.setAttribute("aria-expanded", String(open))
    dial.setAttribute("aria-label", open ? "Close chart controls" : "Open chart controls")
  })
  document.querySelector(".panel-close").addEventListener("click", () => {
    panel.hidden = true
    root.classList.remove("panel-open")
    dial.setAttribute("aria-expanded", "false")
    dial.setAttribute("aria-label", "Open chart controls")
    dial.focus()
  })

  const installCommands = document.querySelectorAll(".copy-command")
  document.querySelectorAll("[data-install]").forEach((button) => {
    button.addEventListener("click", () => {
      document.querySelectorAll("[data-install]").forEach((item) => item.classList.toggle("active", item === button))
      const checkout = button.dataset.install === "checkout"
      const values = checkout
        ? [
            "git clone https://github.com/ryumacodes/dither-kit-compose.git",
            "implementation(project(\":dither-kit-compose\"))",
            "./gradlew :dither-kit-compose:check",
            "./gradlew :sample:run",
          ]
        : [
            "./gradlew :dither-kit-compose:publishToMavenLocal",
            'implementation("io.github.ryumacodes:dither-kit-compose:0.1.0-SNAPSHOT")',
            'include(":dither-kit-compose")',
            "./gradlew check",
          ]
      installCommands.forEach((command, index) => {
        command.dataset.copy = values[index]
        command.querySelector("code").textContent = `${index === 0 || index >= 2 ? "$ " : ""}${values[index]}`
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

const avatarNames = ["ada", "grace", "alan", "edsger", "barbara", "linus", "margaret", "dennis", "radia", "donald", "guido", "brendan"]

document.querySelectorAll(".avatar-row").forEach((row) => {
  avatarNames.forEach((name) => {
    const button = document.createElement("button")
    button.className = "avatar-option"
    button.setAttribute("aria-label", `Use the name ${name}`)
    const canvas = document.createElement("canvas")
    canvas.setAttribute("aria-label", `${name} avatar`)
    drawAvatar(canvas, name)
    button.append(canvas)
    button.addEventListener("click", () => {
      avatarInput.value = name
      drawAvatar(primaryAvatar, name)
    })
    row.append(button)
  })
})

const avatarInput = document.querySelector(".avatar-name")
const primaryAvatar = document.querySelector(".avatar-primary")
drawAvatar(primaryAvatar, avatarInput.value)
avatarInput.addEventListener("input", () => drawAvatar(primaryAvatar, avatarInput.value || "compose"))

document.querySelectorAll(".option-row").forEach((row) => {
  const buttons = [...row.querySelectorAll("button")]
  buttons.forEach((button, index) => {
    button.addEventListener("click", () => {
      const isGradient = row.closest(".gradient-demo")
      const bounds = isGradient ? (index < 4 ? [0, 4] : index < 10 ? [4, 10] : [10, 13]) : (index < 4 ? [0, 4] : index < 10 ? [4, 10] : [10, 11])
      buttons.slice(...bounds).forEach((item) => item.classList.remove("active"))
      button.classList.add("active")
      if (isGradient) {
        isGradient.dataset.direction = buttons.slice(0, 4).find((item) => item.classList.contains("active"))?.textContent
        isGradient.dataset.from = buttons.slice(4, 10).find((item) => item.classList.contains("active"))?.textContent
        isGradient.dataset.to = buttons.slice(10).find((item) => item.classList.contains("active"))?.textContent
        drawGradient(isGradient.querySelector("canvas"))
      }
    })
  })
})

bindControls()
bindCharts()
bindActions()
drawAll()
setupEntranceAnimations()
window.addEventListener("resize", drawAll)
