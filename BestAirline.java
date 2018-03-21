import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;

public class BestAirline
{
    public static class Map extends Mapper<LongWritable, Text, Text, DoubleWritable>
    {	
        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException
        {
            if(key.get() > 0)   //To Skip header
            {
                Configuration conf = context.getConfiguration();
                String source = conf.get("source");
                String destination = conf.get("destination");
                //Convert Input to String
		String line = value.toString();
                //Split the string in words
		String[] words = line.split(",");
		//Arrival Delay
		int arrDelay;
		//Departure Delay
		int depDelay;
                if (words[14].equalsIgnoreCase("NA"))
                {
                    arrDelay = 0;
                }        
                else
                {
                    arrDelay = Integer.parseInt(words[14]);;
                }
		//Departure Delay
		if (words[15].equalsIgnoreCase("NA"))
                {
                    depDelay = 0;
                }        
                else
                {
                    depDelay = Integer.parseInt(words[15]);;
                }
		double totalDelay = arrDelay + depDelay;
                //Set the flight and delay value
                if(words[16].equalsIgnoreCase(source) && words[17].equalsIgnoreCase(destination))
                {
                    context.write(new Text(words[8]),new DoubleWritable(totalDelay));
                }
            }
	}
    }

    public static class Reduce extends Reducer<Text, DoubleWritable, Text, DoubleWritable> 
    {
        @Override
	public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException
        {
            Configuration conf = context.getConfiguration();
            String best = conf.get("best");
            String time = conf.get("time");
            double tm = Double.parseDouble(time);
            double average;
            double total = 0;
            int count= 0;

            for(DoubleWritable value : values)
            {
                total += value.get();
                count++;
            }
            average = total / count;
            if(average < tm)
            {
                conf.set("best", key.toString());
                conf.set("time",average+"");
            }
        }

        @Override
        public void cleanup(Context context) throws IOException, InterruptedException
        {
            Configuration conf = context.getConfiguration();
            String source = conf.get("source");
            String destination = conf.get("destination");
            String best = conf.get("best");
            String time = conf.get("time");
            context.write(new Text("Best Airline to Travell from \""+source+"\" to \""+destination+"\" is \""
            +best+"\" with average delay time of : "), new DoubleWritable(Double.parseDouble(time)));
        }
    }

    public static void main(String[] args) throws Exception
    {
        Configuration conf = new Configuration();
        conf.set("source",args[2]);
        conf.set("destination",args[3]);
        conf.set("best","");
        conf.set("time","9999");
        Job job = new Job(conf);
        job.setNumReduceTasks(1);
        job.setJarByClass(BestAirline.class);
        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
