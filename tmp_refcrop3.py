from PIL import Image
b=Image.open(r"refs\cards_travel.png").convert('RGB')
b.crop((10,55,185,80)).resize((175*9,25*9),Image.LANCZOS).save(r"_ref_travel_caps9x.png")
b.crop((140,112,190,132)).resize((50*12,20*12),Image.LANCZOS).save(r"_ref_loot_label12x.png")
print("ok")
