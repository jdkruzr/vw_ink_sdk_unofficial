package io.github.vwunofficial.ink;

public final class ViwoodsInkConfig {
    public final int renderBatchSize;
    public final int jumpPointCount;
    public final int renderDelayCount;
    public final int dirtyRectPaddingPx;
    public final boolean clipDirtyRectsToView;
    public final boolean invalidateView;
    public final ViwoodsInkListener listener;

    private ViwoodsInkConfig(Builder builder) {
        this.renderBatchSize = builder.renderBatchSize;
        this.jumpPointCount = builder.jumpPointCount;
        this.renderDelayCount = builder.renderDelayCount;
        this.dirtyRectPaddingPx = builder.dirtyRectPaddingPx;
        this.clipDirtyRectsToView = builder.clipDirtyRectsToView;
        this.invalidateView = builder.invalidateView;
        this.listener = builder.listener == null ? ViwoodsInkListener.NONE : builder.listener;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ViwoodsInkConfig defaults() {
        return builder().build();
    }

    public static final class Builder {
        private int renderBatchSize = 2;
        private int jumpPointCount = 1;
        private int renderDelayCount = 0;
        private int dirtyRectPaddingPx = 0;
        private boolean clipDirtyRectsToView = true;
        private boolean invalidateView = true;
        private ViwoodsInkListener listener = ViwoodsInkListener.NONE;

        public Builder renderBatchSize(int renderBatchSize) {
            this.renderBatchSize = Math.max(1, renderBatchSize);
            return this;
        }

        public Builder jumpPointCount(int jumpPointCount) {
            this.jumpPointCount = Math.max(0, jumpPointCount);
            return this;
        }

        public Builder renderDelayCount(int renderDelayCount) {
            this.renderDelayCount = Math.max(0, renderDelayCount);
            return this;
        }

        public Builder dirtyRectPaddingPx(int dirtyRectPaddingPx) {
            this.dirtyRectPaddingPx = Math.max(0, dirtyRectPaddingPx);
            return this;
        }

        public Builder clipDirtyRectsToView(boolean clipDirtyRectsToView) {
            this.clipDirtyRectsToView = clipDirtyRectsToView;
            return this;
        }

        public Builder invalidateView(boolean invalidateView) {
            this.invalidateView = invalidateView;
            return this;
        }

        public Builder listener(ViwoodsInkListener listener) {
            this.listener = listener == null ? ViwoodsInkListener.NONE : listener;
            return this;
        }

        public ViwoodsInkConfig build() {
            return new ViwoodsInkConfig(this);
        }
    }
}
