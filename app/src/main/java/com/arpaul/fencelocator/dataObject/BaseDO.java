package com.arpaul.fencelocator.dataObject;

import java.io.Serializable;

/**
 * Created by Aritra on 6/15/2016.
 */
public class BaseDO implements Serializable, Cloneable {
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
