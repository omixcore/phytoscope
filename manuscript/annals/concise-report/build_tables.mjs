import fs from 'node:fs/promises';
import {Workbook,SpreadsheetFile} from '@oai/artifact-tool';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.dirname(fileURLToPath(import.meta.url));
const data=JSON.parse(await fs.readFile(root+'/supporting-tables-data.json','utf8'));
const wb=Workbook.create();
for(const t of data){
 const s=wb.worksheets.add(t.name);s.showGridLines=false;
 const rows=t.rows.map((r,i)=>r.map(raw=>{const v=raw.replaceAll('*','').replaceAll('`','');return /^\d[\d,]*$/.test(v)&&i>0?Number(v.replaceAll(',','')):v;}));
 const cols=rows[0].length,last=String.fromCharCode(64+cols),end=rows.length+3;
 s.getRange(`A2:${last}2`).merge();s.getRange('A2').values=[[t.name+'. '+t.title]];
 s.getRange(`A4:${last}${end}`).values=rows;
 s.getRange(`A2:${last}${end}`).format.font={name:'Arial',size:7,bold:false,color:'#000000'};
 s.getRange(`A4:${last}${end}`).format.wrapText=true;
 s.getRange(`A4:${last}${end}`).format.verticalAlignment='top';
 s.getRange(`A4:${last}4`).format.fill='#F0F2F3';
 s.getRange(`A4:${last}4`).format.borders={top:{style:'thin',color:'#000000'},bottom:{style:'thin',color:'#000000'}};
 s.getRange(`A${end}:${last}${end}`).format.borders={bottom:{style:'thin',color:'#000000'}};
 for(let j=0;j<cols;j++)s.getRange(`${String.fromCharCode(65+j)}4:${String.fromCharCode(65+j)}${end}`).format.columnWidth=t.name==='Table S2'?(j===0?40:20):cols===2?(j===0?45:105):[34,46,50,54][j];
 s.getRange(`A4:${last}${end}`).format.autofitRows();
 if(t.name==='Table S2'){s.getRange(`B5:D${end}`).setNumberFormat('#,##0');}
 const note=end+2;s.getRange(`A${note}:${last}${note+1}`).merge();s.getRange(`A${note}`).values=[['Source: PhytoScope Supporting Information, evidence snapshot 159ae0f79bd3681afc6c914db282c515190a3888; https://github.com/omixcore/phytoscope']];s.getRange(`A${note}:${last}${note+1}`).format={font:{name:'Arial',size:7,bold:false,color:'#000000'},wrapText:true};
}
wb.recalculate();
console.log((await wb.inspect({kind:'sheet',include:'id,name',maxChars:2000})).ndjson);
console.log((await wb.inspect({kind:'match',searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#NUM!',options:{useRegex:true,maxResults:10},maxChars:1000})).ndjson);
for(const t of data){const b=await wb.render({sheetName:t.name,range:`A1:${String.fromCharCode(64+t.rows[0].length)}${t.rows.length+7}`,scale:2,format:'png'});await fs.writeFile(`/tmp/PhytoScope_${t.name.replaceAll(' ','_')}.png`,new Uint8Array(await b.arrayBuffer()));}
await (await SpreadsheetFile.exportXlsx(wb)).save(root+'/Supporting_Tables.xlsx');
