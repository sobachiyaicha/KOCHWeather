from PIL import Image
a=Image.open(r"refs\clean_main.png").convert('RGB')
print(a.size)
a.crop((20,45,190,80)).resize((170*6,35*6),Image.LANCZOS).save(r"_ref_capsule6x.png")
b=Image.open(r"refs\cards_travel.png").convert('RGB')
b.crop((10,110,190,135)).resize((180*6,25*6),Image.LANCZOS).save(r"_ref_travellabel6x.png")
b.crop((10,125,190,155)).resize((180*6,30*6),Image.LANCZOS).save(r"_ref_traveltitle6x.png")
print("ok")
