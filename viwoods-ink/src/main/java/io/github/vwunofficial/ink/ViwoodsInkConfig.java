package io.github.vwunofficial.ink;

public final class ViwoodsInkConfig {
    public final int renderBatchSize;
    public final int jumpPointCount;
    public final int renderDelayCount;
    public final boolean invalidateView;

    private ViwoodsInkConfig(Builder builder) {
        this.renderBatchSize = builder.renderBatchSize;
        this.jumpPointCount = builder.jumpPointCount;
        this.renderDelayCount = builder.renderDelayCount;
        this.invalidateView = builder.invalidateView;
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
        private boolean invalidateView = true;

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

        public Builder invalidateView(boolean invalidateView) {
            this.invalidateView = invalidateView;
            return this;
        }

        public ViwoodsInkConfig build() {
            return new ViwoodsInkConfig(this);
        }
    }
}
