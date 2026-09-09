package io.github.mmilk23.screenduo.display;

public final class FrameScaler {

    private FrameScaler() {
    }

    public static RgbFrame fit(
            RgbFrame source,
            DisplayGeometry targetGeometry,
            RgbColor background) {
        RgbFrame target = new RgbFrame(targetGeometry);
        FrameCanvas canvas = new FrameCanvas(target);
        canvas.clear(background);

        double scale = Math.min(
                (double) targetGeometry.width() / source.geometry().width(),
                (double) targetGeometry.height() / source.geometry().height());
        int scaledWidth = Math.max(1, (int) Math.round(source.geometry().width() * scale));
        int scaledHeight = Math.max(1, (int) Math.round(source.geometry().height() * scale));
        int offsetX = (targetGeometry.width() - scaledWidth) / 2;
        int offsetY = (targetGeometry.height() - scaledHeight) / 2;

        for (int targetY = 0; targetY < scaledHeight; targetY++) {
            int sourceY = Math.min(
                    source.geometry().height() - 1,
                    (int) (targetY / scale));
            for (int targetX = 0; targetX < scaledWidth; targetX++) {
                int sourceX = Math.min(
                        source.geometry().width() - 1,
                        (int) (targetX / scale));
                target.setPixel(
                        offsetX + targetX,
                        offsetY + targetY,
                        source.redAt(sourceX, sourceY),
                        source.greenAt(sourceX, sourceY),
                        source.blueAt(sourceX, sourceY));
            }
        }
        return target;
    }
}
