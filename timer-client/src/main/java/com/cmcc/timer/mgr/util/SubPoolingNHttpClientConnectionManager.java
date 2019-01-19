package com.cmcc.timer.mgr.util;

import java.util.concurrent.TimeUnit;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.nio.conn.ManagedNHttpClientConnection;
import org.apache.http.nio.conn.NHttpConnectionFactory;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
/** */
@Contract(threading = ThreadingBehavior.SAFE)
public class SubPoolingNHttpClientConnectionManager
       extends PoolingNHttpClientConnectionManager{
	/** */
    public SubPoolingNHttpClientConnectionManager(final ConnectingIOReactor ioreactor) {
        this(ioreactor, getDefaultRegistry());
    }
    /** */
    public SubPoolingNHttpClientConnectionManager(
            final ConnectingIOReactor ioreactor,
            final Registry<SchemeIOSessionStrategy> iosessionFactoryRegistry) {
        this(ioreactor, null, iosessionFactoryRegistry, null);
    }
    /** */
    public SubPoolingNHttpClientConnectionManager(
            final ConnectingIOReactor ioreactor,
            final NHttpConnectionFactory<ManagedNHttpClientConnection> connFactory,
            final DnsResolver dnsResolver) {
        this(ioreactor, connFactory, getDefaultRegistry(), dnsResolver);
    }
    /** */
    public SubPoolingNHttpClientConnectionManager(
            final ConnectingIOReactor ioreactor,
            final NHttpConnectionFactory<ManagedNHttpClientConnection> connFactory) {
        this(ioreactor, connFactory, getDefaultRegistry(), null);
    }
    /** */
    public SubPoolingNHttpClientConnectionManager(
            final ConnectingIOReactor ioreactor,
            final NHttpConnectionFactory<ManagedNHttpClientConnection> connFactory,
            final Registry<SchemeIOSessionStrategy> iosessionFactoryRegistry) {
        this(ioreactor, connFactory, iosessionFactoryRegistry, null);
    }
    /** */
    public SubPoolingNHttpClientConnectionManager(
            final ConnectingIOReactor ioreactor,
            final NHttpConnectionFactory<ManagedNHttpClientConnection> connFactory,
            final Registry<SchemeIOSessionStrategy> iosessionFactoryRegistry,
            final DnsResolver dnsResolver) {
        this(ioreactor, connFactory, iosessionFactoryRegistry, null, dnsResolver,
            15 * 1000, TimeUnit.MILLISECONDS);
    }
    /** */
	public SubPoolingNHttpClientConnectionManager(ConnectingIOReactor ioreactor,
			NHttpConnectionFactory<ManagedNHttpClientConnection> connFactory,
			Registry<SchemeIOSessionStrategy> iosessionFactoryRegistry, SchemePortResolver schemePortResolver,
			DnsResolver dnsResolver, long timeToLive, TimeUnit tunit) {
		super(ioreactor, connFactory, iosessionFactoryRegistry, schemePortResolver, dnsResolver, 15 * 1000, TimeUnit.MILLISECONDS);
	}
	
	
    private static Registry<SchemeIOSessionStrategy> getDefaultRegistry() {
        return RegistryBuilder.<SchemeIOSessionStrategy>create()
                .register("http", NoopIOSessionStrategy.INSTANCE)
                .register("https", SSLIOSessionStrategy.getDefaultStrategy())
                .build();
    }
}
