package io.mapzone.controller.http;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Defines and provides access to an {@link Provision} property.
 * <p/>
 * The value of a context property is shared by all instances within the same scope!
 * By default the <b>scope</b> of the property is the <b>package</b> of the
 * {@link IPanel} class. The {@link Scope} annotation can be used to set/change the
 * scope.
 * 
 * @see Scope
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface Context<T> {

    public boolean isPresent();
    
    public void ifPresent( Consumer<T> consumer );
    
    public T get();
    
    public T __();
    
    public T getOrWait( int time, TimeUnit unit );
    
    public T set( T value );
    
    
    /**
     * Atomically sets the value to the given updated value if the current value
     * {@link Object#equals(Object)} the expected value.
     * 
     * @param expect the expected value
     * @param update the new value
     * @return true if successful. False return indicates that the actual value was
     *         not equal to the expected value.
     */
    public boolean compareAndSet( T expect , T update );
    
    public Class<T> getDeclaredType();
    
    public String getScope();
    
}
