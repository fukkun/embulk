package org.embulk.spi;

import org.embulk.config.Config;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.time.Timestamp;
import org.embulk.type.Column;
import org.embulk.type.SchemaConfig;
import org.embulk.type.Type;

public class MockParserPlugin implements ParserPlugin
{
    public static boolean raiseException = false;

    public interface PluginTask extends Task
    {
        @Config("columns")
        public SchemaConfig getSchemaConfig();
    }

    @Override
    public void transaction(ConfigSource config, Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        control.run(task.dump(), task.getSchemaConfig().toSchema());
    }

    @Override
    public void run(TaskSource taskSource, org.embulk.type.Schema schema,
            FileInput input, org.embulk.spi.PageOutput output)
    {
        try (final PageBuilder pageBuilder = new PageBuilder(
                Exec.getBufferAllocator(), schema, output)) {
            while (input.nextFile()) {
                Buffer buffer = input.poll();
                if (buffer != null) {
                    for (Column column : schema.getColumns()) {
                        Type type = column.getType();
                        switch (type.getName()) {
                        case "boolean":
                            pageBuilder.setBoolean(column, true);
                            break;
                        case "long":
                            pageBuilder.setLong(column, 2L);
                            break;
                        case "double":
                            pageBuilder.setDouble(column, 3.0D);
                            break;
                        case "string":
                            pageBuilder.setString(column, "45");
                            break;
                        case "timestamp":
                            pageBuilder.setTimestamp(column,
                                    Timestamp.ofEpochMilli(678L));
                            break;
                        default:
                            throw new IllegalStateException("Unknown type: "
                                    + type.getName());
                        }
                    }
                    pageBuilder.addRecord();
                    if (raiseException) {
                        throw new RuntimeException("emulated exception");
                    }
                }
            }
            pageBuilder.finish();
        }
    }
}
