package netflix.nebula.dependency.recommender.provider;

import org.gradle.api.Project;

import java.io.*;
import java.util.Collection;
import java.util.Properties;

public class PropertyFileRecommendationProvider extends FileBasedRecommendationProvider {
    
    private volatile Properties recommendations;

    private FuzzyVersionResolver fuzzyResolver = new FuzzyVersionResolver() {
        @Override
        protected Collection<String> propertyNames() {
            return recommendations.stringPropertyNames();
        }

        @Override
        protected String propertyValue(String name) {
            String property = recommendations.getProperty(name);
            if (property == null) {
                return null;
            }
            return property.trim();
        }
    };

    public PropertyFileRecommendationProvider(Project project) {
        super(project);
    }

    @Override
    public String getVersion(String org, String name) throws Exception {
        
        Properties tmpResult = recommendations;
        
        if(tmpResult == null) {
            synchronized (this) {
                if (tmpResult == null) {
                    tmpResult = new Properties();
                    try (InputStream inputStream = inputProvider.getInputStream()) {
                        tmpResult.load(
                                new EolCommentFilteringReader(
                                        new ColonFilteringReader(new InputStreamReader(inputStream))));
                    }
                    recommendations = tmpResult;
                }
            }
        }
        return fuzzyResolver.versionOf(org + "/" + name);
    }

    /**
     * Because unfortunately Properties.load treats colons as an assignment operator
     */
    private class ColonFilteringReader extends Reader {
        Reader reader;

        public ColonFilteringReader(Reader reader) {
            this.reader = reader;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int pos = reader.read(cbuf, off, len);
            for(int i = 0; i < cbuf.length; i++)
                if(cbuf[i] == ':')
                    cbuf[i] = '/';
            return pos;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }

    private class EolCommentFilteringReader extends Reader {
        Reader reader;
        boolean inComment;

        public EolCommentFilteringReader(Reader reader) {
            this.reader = reader;
            inComment = false;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int val;
            int read = 0;
            while (read < len && (val = reader.read()) != -1) {
                if (val == '#') {
                    inComment = true;
                    continue;
                }
                if (val == '\n') {
                    inComment = false;
                }

                if (inComment) {
                    continue;
                }

                cbuf[off + read] = (char) val;
                read++;
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
