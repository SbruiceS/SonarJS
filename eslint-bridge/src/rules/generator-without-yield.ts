/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { Rule } from 'eslint';
import * as estree from 'estree';
import { getMainFunctionTokenLocation } from 'eslint-plugin-sonarjs/lib/utils/locations';
import { Rule as Rule1 } from 'eslint-plugin-sonarjs/lib/utils/types';
import { getParent } from '../utils';
import { TSESTree } from '@typescript-eslint/experimental-utils';

const MESSAGE = 'Add a "yield" statement to this generator.';

export const rule: Rule.RuleModule = {
  create(context: Rule.RuleContext) {
    const yieldStack: number[] = [];

    function enterFunction() {
      yieldStack.push(0);
    }

    function exitFunction(node: estree.Node) {
      const functionNode = node as estree.FunctionExpression | estree.FunctionDeclaration;
      const countYield = yieldStack.pop();
      if (countYield === 0 && functionNode.body.body.length > 0) {
        context.report({
          message: MESSAGE,
          loc: getMainFunctionTokenLocation(
            functionNode as TSESTree.FunctionLike,
            getParent(context) as TSESTree.Node,
            context as unknown as Rule1.RuleContext,
          ),
        });
      }
    }

    return {
      ':function[generator=true]': enterFunction,
      ':function[generator=true]:exit': exitFunction,
      YieldExpression() {
        if (yieldStack.length > 0) {
          yieldStack[yieldStack.length - 1] += 1;
        }
      },
    };
  },
};
