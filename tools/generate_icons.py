"""Generate the warm FamilyChat tree/network mark for native packages."""
from pathlib import Path
from PIL import Image, ImageDraw

assets = Path(__file__).resolve().parents[1] / "assets"
assets.mkdir(exist_ok=True)
size = 512
image = Image.new("RGBA", (size, size), "#f9f7f0")
draw = ImageDraw.Draw(image)
draw.rounded_rectangle((18, 18, 494, 494), radius=105, fill="#e2f6ee", outline="#bddfd1", width=10)
green, ink = "#1c7e5b", "#1c3a33"
draw.line((256, 390, 256, 210), fill=ink, width=32)
draw.line((256, 275, 150, 168), fill=green, width=25)
draw.line((256, 275, 362, 168), fill=green, width=25)
draw.line((256, 230, 256, 126), fill=green, width=25)
for x, y in ((150, 168), (362, 168), (256, 126), (256, 390)):
    draw.ellipse((x-42, y-42, x+42, y+42), fill="#ffffff", outline=green, width=18)
image.save(assets / "familychat.png")
image.save(assets / "familychat.ico", sizes=[(16,16),(32,32),(48,48),(64,64),(128,128),(256,256)])
image.save(assets / "familychat.icns")
