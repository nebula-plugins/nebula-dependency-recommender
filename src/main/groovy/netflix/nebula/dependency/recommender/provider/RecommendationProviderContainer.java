package netflix.nebula.dependency.recommender.provider;

import groovy.lang.Closure;
import netflix.nebula.dependency.recommender.RecommendationStrategies;
import org.gradle.api.Action;
import org.gradle.api.Namer;
import org.gradle.api.Project;
import org.gradle.api.internal.ClosureBackedAction;
import org.gradle.api.internal.ConfigureByMapAction;
import org.gradle.api.internal.DefaultNamedDomainObjectList;

import java.util.HashMap;
import java.util.Map;

public class RecommendationProviderContainer extends DefaultNamedDomainObjectList<RecommendationProvider> {

    private Project project;
    private RecommendationStrategies strategy = RecommendationStrategies.OverrideTransitives;
    
    // Make strategies available without import
    public static final RecommendationStrategies OverrideTransitives = RecommendationStrategies.OverrideTransitives;
    public static final RecommendationStrategies ConflictResolved = RecommendationStrategies.ConflictResolved;

    private final Action<? super RecommendationProvider> addLastAction = new Action<RecommendationProvider>() {
        public void execute(RecommendationProvider r) {
            RecommendationProviderContainer.super.add(r);
        }
    };

    public RecommendationProviderContainer(Project project) {
        super(RecommendationProvider.class, null, new RecommendationProviderNamer());
        this.project = project;
    }

    private static class RecommendationProviderNamer implements Namer<RecommendationProvider> {
        public String determineName(RecommendationProvider r) {
            return r.getName();
        }
    }

    public <T extends RecommendationProvider> T add(T provider, Action<? super T> configureAction) {
        configureAction.execute(provider);
        assertCanAdd(provider.getName());
        addLastAction.execute(provider);
        return provider;
    }

    public <T extends RecommendationProvider> T addFirst(T provider) {
        remove(provider);
        super.add(0, provider);
        return provider;
    }

    public PropertyFileRecommendationProvider propertiesFile(Map<String, ?> args) {
        Map<String, Object> modifiedArgs = new HashMap<String, Object>(args);
        return add(new PropertyFileRecommendationProvider(project), new ConfigureByMapAction<PropertyFileRecommendationProvider>(modifiedArgs));
    }

    public PropertyFileRecommendationProvider propertiesFile(Closure closure) {
        return add(new PropertyFileRecommendationProvider(project), new ClosureBackedAction<PropertyFileRecommendationProvider>(closure));
    }

    public MavenBomRecommendationProvider mavenBom(Map<String, ?> args) {
        Map<String, Object> modifiedArgs = new HashMap<String, Object>(args);
        return add(new MavenBomRecommendationProvider(project), new ConfigureByMapAction<MavenBomRecommendationProvider>(modifiedArgs));
    }

    public MavenBomRecommendationProvider mavenBom(Closure closure) {
        return add(new MavenBomRecommendationProvider(project), new ClosureBackedAction<MavenBomRecommendationProvider>(closure));
    }

    public IvyRecommendationProvider ivyXml(Map<String, ?> args) {
        Map<String, Object> modifiedArgs = new HashMap<String, Object>(args);
        return add(new IvyRecommendationProvider(project), new ConfigureByMapAction<IvyRecommendationProvider>(modifiedArgs));
    }

    public IvyRecommendationProvider ivyXml(Closure closure) {
        return add(new IvyRecommendationProvider(project), new ClosureBackedAction<IvyRecommendationProvider>(closure));
    }

    public DependencyLockProvider dependencyLock(Map<String, ?> args) {
        Map<String, Object> modifiedArgs = new HashMap<String, Object>(args);
        return add(new DependencyLockProvider(project), new ConfigureByMapAction<DependencyLockProvider>(modifiedArgs));
    }

    public DependencyLockProvider dependencyLock(Closure closure) {
        return add(new DependencyLockProvider(project), new ClosureBackedAction<DependencyLockProvider>(closure));
    }

    public MapRecommendationProvider map(Map<String, ?> args) {
        Map<String, Object> modifiedArgs = new HashMap<String, Object>(args);
        return add(new MapRecommendationProvider(), new ConfigureByMapAction<MapRecommendationProvider>(modifiedArgs));
    }

    public MapRecommendationProvider map(Closure closure) {
        return add(new MapRecommendationProvider(), new ClosureBackedAction<MapRecommendationProvider>(closure));
    }

    public CustomRecommendationProvider add(Closure closure) {
        return add(new CustomRecommendationProvider(closure), new Action<CustomRecommendationProvider>() {
            @Override
            public void execute(CustomRecommendationProvider customRecommendationProvider) {
            }
        });
    }

    public String getRecommendedVersion(String group, String name) {
        RecommendationProviderContainer ext = project
                .getRootProject()
                .getExtensions()
                .getByType(RecommendationProviderContainer.class);

        // providers are queried in LIFO order
        for (int i = ext.size()-1; i >= 0; i--) {
            try {
                String version = ext.get(i).getVersion(project.getName(), group, name);
                if(version != null)
                    return version;
            } catch(Exception e) {
                project.getLogger().error("Exception while polling provider " + get(i).getName() + " for version", e);
            }
        }
        return null;
    }

    public RecommendationStrategies getStrategy() {
        return strategy;
    }

    public void setStrategy(RecommendationStrategies strategy) {
        this.strategy = strategy;
    }
}
