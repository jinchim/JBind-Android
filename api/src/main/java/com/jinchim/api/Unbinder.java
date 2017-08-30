package com.jinchim.api;



public interface Unbinder {

    void unbind();

    Unbinder Empty = new Unbinder() {
        @Override
        public void unbind() {
        }
    };

}
