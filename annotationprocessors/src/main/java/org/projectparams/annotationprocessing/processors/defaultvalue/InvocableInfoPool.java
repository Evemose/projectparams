package org.projectparams.annotationprocessing.processors.defaultvalue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InvocableInfoPool {
    private final List<InvocableInfo> invocableInfos = new ArrayList<>();

    public static InvocableInfoPool of(InvocableInfo... invocableInfos) {
        var pool = new InvocableInfoPool();
        pool.addAll(List.of(invocableInfos));
        return pool;
    }

    public void add(InvocableInfo invocableInfo) {
        invocableInfos.add(invocableInfo);
    }

    public void addAll(List<InvocableInfo> invocableInfos) {
        this.invocableInfos.addAll(invocableInfos);
    }

    public void forEach(Consumer<InvocableInfo> consumer) {
        invocableInfos.forEach(consumer);
    }

    @Override
    public String toString() {
        return String.join("\n", invocableInfos.stream().map(InvocableInfo::toString).toList());
    }
}
