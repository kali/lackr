#!/usr/bin/ruby

require "rubygems"
require "right_aws"
require "resolv"
require 'net/https'
require 'uri'
require 'swissr'
require 'rexml/document'

BUCKET="fotonauts.infra"
PATH="lackr/"


version = nil
pom = REXML::Document.new(File.new("pom.xml"))
pom.elements.each('project/version') do |v| version = v.text end

task :default => :push_jar

desc "push jar to s3"
task :push_jar do
  puts "Connecting to S3"
  params     = { :port => 80, :protocol => 'http' }
  aki        = Swissr['/gems/aws-credentials/access_key_id']
  sak        = Swissr['/gems/aws-credentials/secret_access_key']
  gem_bucket = BUCKET

  file_name = "lackr-#{version}-jar-with-dependencies.jar"
  s3_path = PATH + file_name
  file_path = "target/" + file_name

  conn    = Rightscale::S3.new(aki, sak, params)
  bucket  = conn.bucket(gem_bucket, false)
  key     = RightAws::S3::Key.create(bucket, s3_path)

  data    = File.read(file_path)
  b64_md5 = Base64.encode64(Digest::MD5.new.update(data).digest).strip

  puts "Uploading #{file_name} to #{BUCKET}"
  key.put(data, nil, { 'content-md5' => b64_md5 })
  puts "Upload of #{file_name} finished"
        
  puts "Fixing ACL for #{file_name}"
  # give full control to 'testing' account
  RightAws::S3::Grantee.new(key, '2eb598fd4a92fe58b8c979dfbca5a08ae9aef831925715416dde1c42ee29a4ac', 'FULL_CONTROL', :apply)

  puts "Creating pre-signed url for gem server:"
  intf = Rightscale::S3Interface.new(aki, sak, params)
  link = intf.get_link(BUCKET, s3_path, expires = 3600 * 24 * 120 )

  puts ""
  puts "crap to cut and paste to cookr"
  puts "-----------8<------------------------------------------------------"
  puts "lackr_url='#{link}'"
  puts "lackr_version='#{version}'"
  puts "-----------8<------------------------------------------------------"

end
