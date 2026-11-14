from PIL import Image, ImageEnhance
im=Image.open(r"shots\r9_06_lootbox_FAIL_sheet_not_opened.png").convert('RGB')
c=im.crop((600,1240,1080,1520))
c=ImageEnhance.Contrast(ImageEnhance.Brightness(c).enhance(3.2)).enhance(1.6)
c.resize((480*2,280*2),Image.LANCZOS).save(r"_loot_ghost_bright.png")
c2=im.crop((60,1240,600,1520))
c2=ImageEnhance.Contrast(ImageEnhance.Brightness(c2).enhance(3.2)).enhance(1.6)
c2.resize((540*2,280*2),Image.LANCZOS).save(r"_loot_ghost_left.png")
print("ok")
