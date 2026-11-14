from PIL import Image
im=Image.open(r"shots\r9_03_roulette.png").convert('RGB')
# crop phone area and upscale
im.crop((320,440,760,1000)).resize((880,1120),Image.LANCZOS).save(r"_delta_phone2x.png")
im.crop((150,1100,1000,1330)).resize((850*2,230*2),Image.LANCZOS).save(r"_delta_status_chips.png")
print("ok")
