#!/usr/bin/env python3
"""
Generates the Armored-Elytra-style inventory-icon assets for the modded
chestplate/glider combinations, mirroring ArmoredElytraDataGenerator exactly:

  A. Vanilla elytra + enderite chestplate  (all trim materials)
  B. Vanilla elytra + vanilla chestplate + enderite trim material
  C. Enderite (seperated) elytra + any chestplate + any trim material

Textures are shifted the same way AE shifts them (elytra DOWN h/8, chestplate
UP h/8) and the enderite trim overlay is palette-mapped from AE's shifted
grayscale trim template + the enderite color palette. Vanilla trim overlays are
reused from AE's own generated resources; only the enderite deltas are new.
"""
import json
import os
from PIL import Image

SRC = "/tmp/claude-1000/-home-x/2b36e0b9-cc31-41a8-b1aa-11d2950b48fa/scratchpad/icongen"
RES = "/home/x/claude-files/elytra-compat/elytracompat/src/main/resources"
AE_ELYTRA_JSON = "/home/x/claude-files/elytra-compat/armored-elytra/src/client/generated/resources/assets/minecraft/items/elytra.json"
EN_SEP_JSON_JAR = None  # read below

TEX_OUT = os.path.join(RES, "assets/elytracompat/textures/item")
MODEL_OUT = os.path.join(RES, "assets/elytracompat/models/item")
os.makedirs(TEX_OUT, exist_ok=True)
os.makedirs(MODEL_OUT, exist_ok=True)
os.makedirs(os.path.join(RES, "assets/minecraft/items"), exist_ok=True)
os.makedirs(os.path.join(RES, "assets/enderitemod/items"), exist_ok=True)

# ----------------------------------------------------------------------------
# image helpers (ports of ArmoredElytraDataGenerator)
# ----------------------------------------------------------------------------

def load_rgba(path):
    return Image.open(path).convert("RGBA")

def shift(img, delta):
    """Positive delta = move content DOWN; negative = UP. Matches AE.shiftImage."""
    w, h = img.size
    dst = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    dp = dst.load()
    for y in range(h):
        sy = y - delta
        if sy < 0 or sy >= h:
            continue
        for x in range(w):
            dp[x, y] = px[x, sy]
    return dst

def shift_by_eighth(img, down):
    d = img.size[1] // 8
    return shift(img, d if down else -d)

def palette_colors(path):
    im = Image.open(path).convert("RGBA")
    w, _ = im.size
    px = im.load()
    return [px[i, 0] for i in range(w)]

def apply_palette(template, key_colors, target_colors):
    """Port of AE.applyPalette: gray value -> target color via the key strip."""
    w, h = template.size
    dst = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    tp = template.load()
    dp = dst.load()
    gray_to_color = {}
    for i in range(min(len(key_colors), len(target_colors))):
        gray = key_colors[i][0]  # R channel of grayscale key
        gray_to_color[gray] = target_colors[i]
    for y in range(h):
        for x in range(w):
            r, g, b, a = tp[x, y]
            if a == 0:
                continue
            mapped = gray_to_color.get(r)
            if mapped is None:
                continue
            mr, mg, mb, ma = mapped
            out_a = a if ma > 0 else 0
            dp[x, y] = (mr, mg, mb, out_a)
    return dst

def save_png(img, name):
    img.save(os.path.join(TEX_OUT, name + ".png"))

# ----------------------------------------------------------------------------
# 1. textures
# ----------------------------------------------------------------------------

# enderite chestplate: shift UP (down=False), like AE chestplates
save_png(shift_by_eighth(load_rgba(os.path.join(SRC, "en/assets/enderitemod/textures/item/enderite_chestplate.png")), False),
         "enderite_chestplate")

# enderite (seperated) elytra base: shift DOWN, like AE elytra
save_png(shift_by_eighth(load_rgba(os.path.join(SRC, "en/assets/enderitemod/textures/item/enderite_elytra_seperated.png")), True),
         "enderite_elytra_base")
save_png(shift_by_eighth(load_rgba(os.path.join(SRC, "en/assets/enderitemod/textures/item/enderite_elytra_seperated_broken.png")), True),
         "enderite_elytra_broken_base")

# enderite trim overlays (base + darker) from AE's shifted grayscale template
template = load_rgba(os.path.join(SRC, "ae_chestplate_trim.png"))
key = palette_colors(os.path.join(SRC, "mc/assets/minecraft/textures/trims/color_palettes/trim_palette.png"))
save_png(apply_palette(template, key, palette_colors(os.path.join(SRC, "en/assets/enderitemod/textures/trims/color_palettes/enderite.png"))),
         "chestplate_trim_enderite")
save_png(apply_palette(template, key, palette_colors(os.path.join(SRC, "en/assets/enderitemod/textures/trims/color_palettes/enderite_darker.png"))),
         "chestplate_trim_enderite_darker")

# ----------------------------------------------------------------------------
# 2. data model
# ----------------------------------------------------------------------------

# (registered trim-material id, short name, [chestplates that use the _darker palette])
MATERIALS = [
    ("minecraft:amethyst", "amethyst", []),
    ("minecraft:copper", "copper", ["copper_chestplate"]),
    ("minecraft:diamond", "diamond", ["diamond_chestplate"]),
    ("minecraft:emerald", "emerald", []),
    ("minecraft:gold", "gold", ["golden_chestplate"]),
    ("minecraft:iron", "iron", ["iron_chestplate"]),
    ("minecraft:lapis", "lapis", []),
    ("minecraft:netherite", "netherite", ["netherite_chestplate"]),
    ("minecraft:quartz", "quartz", []),
    ("minecraft:redstone", "redstone", []),
    ("minecraft:resin", "resin", []),
    ("enderitemod:enderite", "enderite", ["enderite_chestplate"]),
]

# vanilla chestplates AE supports, plus the enderite chestplate
VANILLA_CHESTS = ["chainmail_chestplate", "copper_chestplate", "diamond_chestplate",
                  "golden_chestplate", "iron_chestplate", "leather_chestplate",
                  "netherite_chestplate"]
ALL_CHESTS = VANILLA_CHESTS + ["enderite_chestplate"]

def chest_item_id(chest):
    return ("enderitemod:" if chest == "enderite_chestplate" else "minecraft:") + chest

def chest_layer(chest):
    return ("elytracompat:item/enderite_chestplate" if chest == "enderite_chestplate"
            else "armored_elytra:item/" + chest)

def trim_layer(chest, mat_short, darker_for):
    darker = chest in darker_for
    if mat_short == "enderite":
        return "elytracompat:item/chestplate_trim_enderite" + ("_darker" if darker else "")
    # reuse AE's own generated vanilla trim overlays
    return "armored_elytra:item/chestplate_trim_" + mat_short + ("_darker" if darker else "")

def build_model(base_layer0, chest, mat=None):
    textures = {"layer0": base_layer0, "layer1": chest_layer(chest)}
    n = 2
    if chest == "leather_chestplate":
        textures["layer%d" % n] = "armored_elytra:item/leather_chestplate_overlay"
        n += 1
    if mat is not None:
        _id, short, darker_for = mat
        textures["layer%d" % n] = trim_layer(chest, short, darker_for)
    return {"parent": "minecraft:item/generated", "textures": textures}

def write_model(name, model):
    with open(os.path.join(MODEL_OUT, name + ".json"), "w") as f:
        json.dump(model, f, indent=2)

def model_ref(node):
    return {"type": "minecraft:model", "model": node}

def trim_condition(mat_id, on_true_model, on_false):
    return {
        "type": "minecraft:condition",
        "property": "minecraft:component",
        "predicate": "minecraft:custom_data",
        "value": {"armored_elytra:trim_material": mat_id},
        "on_true": model_ref(on_true_model),
        "on_false": on_false,
    }

def build_trim_tree(model_name_for, plain_model):
    """Nested condition chain over every material, plain model as final fallback."""
    node = model_ref(plain_model)
    for mat_id, short, _ in reversed(MATERIALS):
        node = trim_condition(mat_id, model_name_for(short), node)
    return node

# ----------------------------------------------------------------------------
# 3. models - vanilla-elytra base (A + B)
# ----------------------------------------------------------------------------

ELYTRA0 = "armored_elytra:item/elytra"

# A: enderite chestplate, plain + every trim
write_model("elytra_enderite_chestplate", build_model(ELYTRA0, "enderite_chestplate"))
for mat in MATERIALS:
    write_model("elytra_enderite_chestplate_%s_trim" % mat[1],
                build_model(ELYTRA0, "enderite_chestplate", mat))

# B: vanilla chestplates, enderite trim only (AE already ships vanilla+vanilla)
ENDERITE_MAT = MATERIALS[-1]
for chest in VANILLA_CHESTS:
    write_model("elytra_%s_enderite_trim" % chest, build_model(ELYTRA0, chest, ENDERITE_MAT))

# ----------------------------------------------------------------------------
# 4. models - enderite-elytra base (C)
# ----------------------------------------------------------------------------

ENELYTRA0 = "elytracompat:item/enderite_elytra_base"
write_model("enderiteelytra_broken", {"parent": "minecraft:item/generated",
            "textures": {"layer0": "elytracompat:item/enderite_elytra_broken_base"}})
for chest in ALL_CHESTS:
    write_model("enderiteelytra_%s" % chest, build_model(ENELYTRA0, chest))
    for mat in MATERIALS:
        write_model("enderiteelytra_%s_%s_trim" % (chest, mat[1]),
                    build_model(ENELYTRA0, chest, mat))

# ----------------------------------------------------------------------------
# 5. elytra.json override (extend AE's tree)
# ----------------------------------------------------------------------------

with open(AE_ELYTRA_JSON) as f:
    elytra_def = json.load(f)

def deepest_on_false(node):
    """Return (parent, key) of the innermost plain-model on_false in a chain."""
    cur = node
    parent, key = None, None
    while isinstance(cur, dict) and cur.get("type") == "minecraft:condition":
        parent, key = cur, "on_false"
        cur = cur["on_false"]
    return parent, key

# find the custom_model_data select node
def find_select(node):
    if isinstance(node, dict):
        if node.get("property") == "minecraft:custom_model_data":
            return node
        for v in node.values():
            r = find_select(v)
            if r:
                return r
    return None

select = find_select(elytra_def)

# B: append an enderite-trim condition to every existing vanilla chestplate case
for case in select["cases"]:
    chest = case["when"].split(":", 1)[1]
    parent, key = deepest_on_false(case["model"])
    plain = parent[key] if parent else case["model"]
    new_cond = trim_condition("enderitemod:enderite",
                              "elytracompat:item/elytra_%s_enderite_trim" % chest, plain)
    if parent:
        parent[key] = new_cond
    else:
        case["model"] = new_cond

# A: add the enderite chestplate case
select["cases"].append({
    "when": "enderitemod:enderite_chestplate",
    "model": build_trim_tree(lambda s: "elytracompat:item/elytra_enderite_chestplate_%s_trim" % s,
                             "elytracompat:item/elytra_enderite_chestplate"),
})

with open(os.path.join(RES, "assets/minecraft/items/elytra.json"), "w") as f:
    json.dump(elytra_def, f, indent=2)

# ----------------------------------------------------------------------------
# 6. enderite_elytra_seperated.json override (C) - wrap, delegate to original
# ----------------------------------------------------------------------------
import zipfile

EJ = "/home/x/claude-files/elytra-compat/elytracompat/libs/enderitemod-1.9.0.jar"
with zipfile.ZipFile(EJ) as z:
    en_original = json.loads(z.read("assets/enderitemod/items/enderite_elytra_seperated.json"))

def chest_case_model(chest):
    """broken -> plain broken; else trim tree over enderiteelytra_<chest>_* models."""
    trim_tree = build_trim_tree(
        lambda s, c=chest: "elytracompat:item/enderiteelytra_%s_%s_trim" % (c, s),
        "elytracompat:item/enderiteelytra_%s" % chest)
    return {
        "type": "minecraft:condition",
        "property": "minecraft:broken",
        "on_true": model_ref("elytracompat:item/enderiteelytra_broken"),
        "on_false": trim_tree,
    }

en_override = {
    "model": {
        "type": "minecraft:select",
        "property": "minecraft:custom_model_data",
        "cases": [{"when": chest_item_id(c), "model": chest_case_model(c)} for c in ALL_CHESTS],
        # unarmored enderite elytra -> exactly what enderitemod shipped
        "fallback": en_original["model"],
    }
}
with open(os.path.join(RES, "assets/enderitemod/items/enderite_elytra_seperated.json"), "w") as f:
    json.dump(en_override, f, indent=2)

# ----------------------------------------------------------------------------
print("textures:", len(os.listdir(TEX_OUT)))
print("models:", len(os.listdir(MODEL_OUT)))
print("done")
