package org.jboss.protean.arc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.spi.CreationalContext;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public class CreationalContextImpl<T> implements CreationalContext<T> {

    private final CreationalContextImpl<?> parent;

    private final List<InstanceHandle<?>> dependentInstances;

    public CreationalContextImpl() {
        this(null);
    }

    public CreationalContextImpl(CreationalContextImpl<?> parent) {
        this.parent = parent;
        this.dependentInstances = Collections.synchronizedList(new ArrayList<>());
    }

    public <I> void addDependentInstance(InjectableBean<I> bean, I instance, CreationalContext<I> ctx) {
        addDependentInstance(new InstanceHandleImpl<I>(bean, instance, ctx));
    }

    public <I> void addDependentInstance(InstanceHandle<I> instanceHandle) {
        dependentInstances.add(instanceHandle);
    }

    void destroyDependentInstance(Object dependentInstance) {
        synchronized (dependentInstances) {
            for (Iterator<InstanceHandle<?>> iterator = dependentInstances.iterator(); iterator.hasNext();) {
                InstanceHandle<?> instanceHandle = iterator.next();
                if (instanceHandle.get() == dependentInstance) {
                    instanceHandle.destroy();
                    iterator.remove();
                    break;
                }
            }
        }
    }

    @Override
    public void push(T incompleteInstance) {
        // No-op
    }

    @Override
    public void release() {
        synchronized (dependentInstances) {
            for (InstanceHandle<?> instance : dependentInstances) {
                instance.destroy();
            }
        }
    }

    public CreationalContextImpl<?> getParent() {
        return parent;
    }

    public <C> CreationalContextImpl<C> child() {
        return new CreationalContextImpl<>(this);
    }

    public static <T> CreationalContextImpl<T> unwrap(CreationalContext<T> ctx) {
        if (ctx instanceof CreationalContextImpl) {
            return (CreationalContextImpl<T>) ctx;
        } else {
            throw new IllegalArgumentException("Failed to unwrap CreationalContextImpl: " + ctx);
        }
    }

    public static <C> CreationalContextImpl<C> child(CreationalContext<?> creationalContext) {
        return unwrap(creationalContext).child();
    }

    public static <I> void addDependencyToParent(InjectableBean<I> bean, I instance, CreationalContext<I> ctx) {
        CreationalContextImpl<?> parent = unwrap(ctx).getParent();
        if (parent != null) {
            parent.addDependentInstance(bean, instance, ctx);
        }
    }

}
