package winky.photograph.model;

import android.os.Parcel;
import android.os.Parcelable;

public class TakePhotoOptions implements Parcelable {

    /**
     * 是否使用TakePhoto自带的相册进行图片选择，默认不使用，但选择多张图片会使用
     */
    private boolean withOwnGallery;
    /**
     * 是对拍的照片进行旋转角度纠正
     */
    private boolean correctImage;

    private TakePhotoOptions() {
    }

    public boolean isWithOwnGallery() {
        return withOwnGallery;
    }

    public void setWithOwnGallery(boolean withOwnGallery) {
        this.withOwnGallery = withOwnGallery;
    }

    public boolean isCorrectImage() {
        return correctImage;
    }

    public void setCorrectImage(boolean correctImage) {
        this.correctImage = correctImage;
    }

    public static class Builder {
        private TakePhotoOptions options;

        public Builder() {
            this.options = new TakePhotoOptions();
        }

        public Builder setWithOwnGallery(boolean withOwnGallery) {
            options.setWithOwnGallery(withOwnGallery);
            return this;
        }

        public Builder setCorrectImage(boolean isCorrectImage) {
            options.setCorrectImage(isCorrectImage);
            return this;
        }

        public TakePhotoOptions create() {
            return options;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.withOwnGallery ? (byte) 1 : (byte) 0);
        dest.writeByte(this.correctImage ? (byte) 1 : (byte) 0);
    }

    protected TakePhotoOptions(Parcel in) {
        this.withOwnGallery = in.readByte() != 0;
        this.correctImage = in.readByte() != 0;
    }

    public static final Parcelable.Creator<TakePhotoOptions> CREATOR = new Parcelable.Creator<TakePhotoOptions>() {
        @Override
        public TakePhotoOptions createFromParcel(Parcel source) {
            return new TakePhotoOptions(source);
        }

        @Override
        public TakePhotoOptions[] newArray(int size) {
            return new TakePhotoOptions[size];
        }
    };
}
