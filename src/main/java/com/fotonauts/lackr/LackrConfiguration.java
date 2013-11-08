package com.fotonauts.lackr;

import java.net.UnknownHostException;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.HttpCookieStore;

import com.fotonauts.commons.RapportrService;
import com.fotonauts.lackr.backend.Backend;
import com.fotonauts.lackr.backend.client.ClientBackend;
import com.fotonauts.lackr.backend.hashring.HashRing;
import com.fotonauts.lackr.backend.inprocess.InProcessFemtor;
import com.fotonauts.lackr.backend.trypass.TryPassBackend;
import com.fotonauts.lackr.esi.FemtorJSESIRule;
import com.fotonauts.lackr.esi.HttpESIRule;
import com.fotonauts.lackr.esi.JSESIRule;
import com.fotonauts.lackr.esi.JSEscapedMLESIRule;
import com.fotonauts.lackr.esi.JSMLESIRule;
import com.fotonauts.lackr.esi.MLESIRule;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.interpolr.Rule;
import com.fotonauts.lackr.interpolr.SimpleSubstitutionRule;
import com.fotonauts.lackr.mustache.ArchiveRule;
import com.fotonauts.lackr.mustache.DumpArchiveRule;
import com.fotonauts.lackr.mustache.EvalRule;
import com.fotonauts.lackr.mustache.TemplateRule;
import com.fotonauts.lackr.picorassets.AssetPrefixRule;
import com.fotonauts.lackr.picorassets.PicorAssetResolver;
import com.mongodb.MongoException;

public class LackrConfiguration {

    private CompositeConfiguration propertySource;
    private Backend femtorBackend;
    private HttpClient client;
    private InterpolrProxy service;
    private Backend varnishAndPicorBackend;
    private RapportrService rapportrService;

    public LackrConfiguration() throws ConfigurationException {
        propertySource = new CompositeConfiguration();
        propertySource.addConfiguration(new SystemConfiguration());
        propertySource.addConfiguration(new PropertiesConfiguration("lackr.default.properties"));
    }

    public PicorAssetResolver buildAssetResolver() {
        PicorAssetResolver resolver = new PicorAssetResolver();
        resolver.setMagicPrefix(propertySource.getString("lackr.assets.magicPrefix"));
        resolver.setCdnPrefix(propertySource.getString("lackr.assets.cdnPrefix"));
        resolver.setAssetDirectoryPath(propertySource.getString("lackr.assets.assetDirectoryPath"));
        return resolver;
    }

    protected RapportrService buildRapportrService() throws NumberFormatException, UnknownHostException, MongoException, Exception {
        RapportrService rapportrService = new RapportrService();
        rapportrService.setFacility("lackr");
        rapportrService.setGrid(propertySource.getString("lackr.rapportr.grid"));
        rapportrService.setIrcErrorChannel(propertySource.getString("lackr.rapportr.channel"));
        rapportrService.setMongoRapportrQueuePath(propertySource.getString("lackr.rapportr.mongo"));
        rapportrService.setMongoAccessLogPath(propertySource.getString("lackr.mongolog.access"));
        return rapportrService;
    }

    public final RapportrService getRapportrService() throws Exception {
        if (rapportrService == null)
            rapportrService = buildRapportrService();
        return rapportrService;
    }

    public final InterpolrProxy getLackrService() throws Exception {
        if (service == null) {
            service = new InterpolrProxy();
            service.setTimeout(propertySource.getInt("lackr.timeout"));
            service.setInterpolr(buildInterpolr());
//            service.setGraphiteHostAndPort(propertySource.getString("lackr.graphite"));
//            service.setGrid(propertySource.getString("lackr.grid"));
//            service.setRapportr(getRapportrService());
            service.setBackend(new TryPassBackend(getFemtorBackend(), getVarnishPicorBackend()));
        }
        return service;
    }

    private Interpolr buildInterpolr() {
        Interpolr interpolr = new Interpolr();
        SimpleSubstitutionRule assetRuleHttp = new SimpleSubstitutionRule("http://_A_S_S_E_T_S___P_A_T_H_",
                propertySource.getString("lackr.assets_prefix"));
        SimpleSubstitutionRule assetRuleHttps = new SimpleSubstitutionRule("https://_A_S_S_E_T_S___P_A_T_H_",
                propertySource.getString("lackr.assets_prefix_ssl"));
        AssetPrefixRule assetPrefixRule = new AssetPrefixRule();
        assetPrefixRule.setResolver(buildAssetResolver());
        interpolr.setRules(new Rule[] { new DumpArchiveRule(), new ArchiveRule(), new TemplateRule(), new EvalRule(),
                assetRuleHttp, assetRuleHttps, new HttpESIRule(), new FemtorJSESIRule(), new JSESIRule(), new JSEscapedMLESIRule(),
                new JSMLESIRule(), new MLESIRule(), assetPrefixRule });
        return interpolr;
    }

    protected final Backend getVarnishPicorBackend() throws Exception {
        if (varnishAndPicorBackend == null) {
            varnishAndPicorBackend = buildVarnishAndPicorBackend();
        }
        return varnishAndPicorBackend;
    }

    protected Backend buildVarnishAndPicorBackend() throws Exception {
        ClientBackend varnishAndPicorBackend = new ClientBackend();
        varnishAndPicorBackend.setActualClient(getJettyClient());
        HashRing hashring = new HashRing(propertySource.getStringArray("lackr.backends"));
        hashring.setProbeUrl("lackr.probeUrl");
        varnishAndPicorBackend.setDirector(hashring);
        return varnishAndPicorBackend;
    }

    protected final Backend getFemtorBackend() throws Exception {
        if (femtorBackend == null)
            femtorBackend = buildFemtorBackend();

        return femtorBackend;
    }

    protected Backend buildFemtorBackend() throws Exception {
        if ("Http".equals(propertySource.getString("lackr.femtorImpl")))
            return buildFemtorBackendHttp();
        else
            return buildFemtorBackendInprocess();
    }

    protected InProcessFemtor buildFemtorBackendInprocess() throws Exception {
        InProcessFemtor be = new InProcessFemtor();
        be.setFemtorHandlerClass(propertySource.getString("lackr.femtorClass"));
        be.setfemtorJar(propertySource.getString("lackr.femtorJar"));
        be.init();
        return be;
    }

    protected ClientBackend buildFemtorBackendHttp() throws Exception {
        ClientBackend backend = new ClientBackend();
        backend.setActualClient(getJettyClient());
        HashRing hashring = new HashRing(propertySource.getStringArray("lackr.femtorBackend"));
        hashring.setProbeUrl("lackr.probeUrl");
        backend.setDirector(hashring);
        return backend;
    }

    protected final HttpClient getJettyClient() throws Exception {
        if (client == null) {
            client = new HttpClient();
            client.setConnectTimeout(500);
            client.setFollowRedirects(false);
            client.setMaxRequestsQueuedPerDestination(16384);
            client.setCookieStore(new HttpCookieStore.Empty());
            client.start();
        }
        return client;
    }

    public void close() throws Exception {
        if (rapportrService != null)
            rapportrService.stop();
        if (service != null)
            service.stop();
        if (femtorBackend != null)
            femtorBackend.stop();
        if (varnishAndPicorBackend != null)
            varnishAndPicorBackend.stop();
        if (client != null) {
            client.stop();
            client.destroy();
        }
    }

    public Configuration getPropertySource() {
        return propertySource;
    }

}
