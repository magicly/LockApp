from PIL import Image
import os

def create_icons(source_path):
    # Android 图标尺寸配置
    sizes = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192,
    }

    # 打开原图
    img = Image.open(source_path)

    # 确保是正方形
    size = max(img.size)
    new_img = Image.new('RGBA', (size, size), (0, 0, 0, 0))

    # 居中粘贴
    x = (size - img.size[0]) // 2
    y = (size - img.size[1]) // 2
    new_img.paste(img, (x, y))

    # 创建输出目录
    base_dir = "app/src/main/res"

    for folder, icon_size in sizes.items():
        # 创建目录
        output_dir = os.path.join(base_dir, folder)
        os.makedirs(output_dir, exist_ok=True)

        # 调整大小
        resized = new_img.resize((icon_size, icon_size), Image.Resampling.LANCZOS)

        # 保存
        output_path = os.path.join(output_dir, "ic_launcher.png")
        resized.save(output_path, "PNG")
        print(f"✓ Created: {output_path} ({icon_size}x{icon_size})")

    print("\n✅ 图标生成完成！")
    print("文件位置: app/src/main/res/mipmap-*/ic_launcher.png")

if __name__ == "__main__":
    # 使用方法
    source = "aiyan.png"  # 你的原图文件名

    if os.path.exists(source):
        create_icons(source)
    else:
        print(f"❌ 找不到文件: {source}")
        print("请确保 aiyan.png 在当前目录")
        