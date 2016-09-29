package com.virjar.ipproxy.ippool.stratege;

import java.util.List;

import com.virjar.model.AvProxy;

/**
 * Created by virjar on 16/9/29.
 */
public interface Importer {

    List<AvProxy> importProxy(String domain, String testUrl);
}
