package com.jinchim.jbind_sdk;



public interface Unbinder {

    void unbind();

    Unbinder Empty = new Unbinder() {
        @Override
        public void unbind() {
        }
    };

}
