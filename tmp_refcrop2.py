from PIL import Image
a=Image.open(r"refs\clean_main.png").convert('RGB')
a.crop((20,68,190,96)).resize((170*8,28*8),Image.LANCZOS).save(r"_ref_capsule_mm8x.png")
b=Image.open(r"refs\cards_travel.png").convert('RGB')
b.crop((120,112,190,140)).resize((70*10,28*10),Image.LANCZOS).save(r"_ref_travel_right10x.png")
b.crop((10,136,190,160)).resize((180*8,24*8),Image.LANCZOS).save(r"_ref_travel_title8x.png")
print("ok")
