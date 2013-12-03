package com.fotonauts.lackr.interpolr.json;


public class Reference {
    private Archive archive;
    private int id;
    private Object target;
    public Reference(Archive archive, int id) {
        this.archive = archive;
        this.id = id;
        this.target = archive.getObject(id);
    }
    
    public int getId() {
        return id;
    }
    
    public Archive getArchive() {
        return archive;
    }

    public Object getTarget() {
        return target;
    }
    
    @Override
    public int hashCode() {
        return archive.hashCode() ^ id;
    }
}
