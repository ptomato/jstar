import cheerio from "cheerio";
import path from "path";
import { Spec } from "./spec";
import { printSep, loadSpec, saveGrammarResult, saveFile } from "./util";
import { getESVersion, getDir, loadRule } from "./util";
import { ECMAScriptVersion } from "./enum";
import { Grammar, extractSection, generateIdxMap } from "./grammar";
import { extractAlgoClause } from "./algorithm";

const argv = require('yargs')
  .usage("Usage: --target <ECMAScript-version>")
  .option('target', {
    alias: 't',
    default: 'es10',
    describe: 'the version of ECMAScript',
    type: 'string',
  })
  .help()
  .alias('help', 'h')
  .argv;

async function main() {
  try {
    const version = getESVersion(argv.target);
    printSep();
    console.log(`VERSION: ${version}`);
    const resourcePath = path.join(__dirname, "..", "resource");
    const html = await loadSpec(resourcePath, version);
    const rule = await loadRule(resourcePath, version);
    let $ = cheerio.load(html);

    // extract Spec from a ECMAScript html file
    const spec = Spec.from($, rule);

    // extract grammar production
    // let grammar: Grammar = new Grammar();
    // const sections = rule.grammar.sections;
    // for (const section of sections) {
    //   let sectionResult = extractSection({ $, rule, section });
    //   const { lexProds, prods } = sectionResult;
    //   grammar.lexProds = grammar.lexProds.concat(lexProds);
    //   grammar.prods = grammar.prods.concat(prods);
    // }

    // /* generate index map */
    // grammar.idxMap = generateIdxMap(grammar);

    // /* save grammar extract result */
    // saveGrammarResult(resourcePath, version, grammar);

    /**
     * Algorithm
     */

    // getAlgoHeader("await", { $ });
    // const clause = extractAlgoClause({ $, clauseId: "sec-getidentifierreference" });

    // save the information as a JSON file
    const cacheDirPath = getDir(resourcePath, '.cache');
    const jsonPath = path.join(cacheDirPath , 'spec.json');
    const data = JSON.stringify({} /* TODO */);
    saveFile(jsonPath , data);

  } catch (err) {
    // show error messages
    console.log(`[ERROR] ${err.message}`);
  }
}

main();
