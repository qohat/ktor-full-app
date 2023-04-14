ALTER TABLE roles ADD COLUMN permissions TEXT;
ALTER TABLE roles ADD CONSTRAINT unique_name UNIQUE (name);

ALTER TABLE users DROP COLUMN permission_chain;

UPDATE roles set permissions = 'RdCf:RdU:WtU:DltU:UptU:RdPrd:WtPrd:DltPrd:UptPrd:RdSup:WtSup:DltSup:UptSup:RdStg:WtStg:DltStg:UptStg' where name = 'Admin';
UPDATE roles set permissions = 'RdPR:WtPR:UptPR:RdPRNf:RdPRTp:RdRpts:RdPrd:WtPrd:DltPrd:UptPrd:RdSup:WtSup:DltSup:UptSup:RdStg:WtStg:DltStg:UptStg:RdP:WtP:DltP:UptP:ExptRpts:ImptRpts' where name = 'Fidu';
UPDATE roles set permissions = 'RdPR:WtPR:UptPR:RdPrd:WtPrd:DltPrd:UptPrd:RdSup:WtSup:DltSup:UptSup:RdStg:WtStg:DltStg:UptStg:WtObs:RdObs' where name = 'Agent';
UPDATE roles set permissions = 'RdPR:RdPRNf:RdPRTp:WtPR:UptPR:RdRpts:RdPrd:WtPrd:DltPrd:UptPrd:RdSup:WtSup:DltSup:UptSup:RdStg:WtStg:DltStg:UptStg:RdP:WtP:DltP:UptP:ExptRpts:ImptRpts' where name = 'Validator';